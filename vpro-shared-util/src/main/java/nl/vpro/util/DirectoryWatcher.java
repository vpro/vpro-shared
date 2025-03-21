package nl.vpro.util;


import lombok.SneakyThrows;
import lombok.With;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.nio.file.*;
import java.time.Clock;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.stream.Stream;

import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;

import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

import nl.vpro.jmx.MBeans;

import static java.nio.file.StandardWatchEventKinds.*;


/**
 * A utility to harnas {@link WatchService} to watch a directory for set of files. Supporting symlinks and those kind of things.
 * <p>
 * Targeted at watching openshift config maps, which can be provided as a list of files, which are all symlinks to the actual files in subdirectories.
 *
 * @since 5.2
 * @author Michiel Meeuwissen
 */
@Slf4j
public class DirectoryWatcher implements AutoCloseable {

    protected final ScheduledExecutorService executorService = Executors.newScheduledThreadPool(1);

    private final Path directory;
    final WatchService watcher;

    private final Consumer<WatcherEvent> consumer;
    private final Predicate<Path> filter;

    private final Future<?> future;

    final Set<String> watchedTargetDirectories = new ConcurrentSkipListSet<>();
    final Map<String, Path> watchedTargetFiles = new ConcurrentHashMap<>();
    final Map<String, Instant> lastModified = new ConcurrentHashMap<>();

    final AtomicInteger counter = new AtomicInteger();
    private Instant last = null;

    private final Clock clock;


    /**
     * @param directory The directory to watch
     * @param eventConsumer What should happen if a certain file in it changes
     * @param pathConsumer What should happen if a certain file in it changes
     * @param filter An optional filter on files in this directory
     */
    @lombok.Builder
    private DirectoryWatcher(
        @NonNull Path directory,
        @Nullable Consumer<WatcherEvent> eventConsumer,
        @Nullable Consumer<Path> pathConsumer,
        @Nullable Predicate<Path> filter,
        @Nullable Clock clock) throws IOException {
        this.directory = directory;
        this.watcher = directory.getFileSystem().newWatchService();
        if (eventConsumer == null && pathConsumer == null) {
            throw new IllegalArgumentException("Either eventConsumer or pathConsumer should be set");
        }
        this.consumer = eventConsumer != null ? eventConsumer :
            (we) -> pathConsumer.accept((we.resolved));
        this.filter = filter == null ? (p) -> true : filter;
        this.clock = clock == null ? Clock.systemUTC() : clock;
        this.future = watchService();


        try {
            MBeans.registerBean(
                new ObjectName("nl.vpro.util.watcher:directory="  + directory),
                new Admin()
            );
        } catch (MalformedObjectNameException ignored) {
            // ignored, the objectname _is_ not malformed
        }
    }

    private WatchKey register(Path path, WatchService watcher) throws IOException {
        return path.register(watcher, ENTRY_CREATE, ENTRY_MODIFY, ENTRY_DELETE);
    }



    @SneakyThrows
    private Future<?> watchService() {
        register(directory, watcher);

        // check initial set of file for existing symlinks to follow the targets of.
        try (Stream<Path> p = Files.list(directory).filter(filter)) {
            p.forEach(file -> {
                checkSymlink(file, ENTRY_CREATE).ifPresent(resolved -> {
                    try {
                        lastModified.put(pathToKey(resolved), Files.getLastModifiedTime(resolved).toInstant());
                    } catch (IOException e) {
                        log.warn(e.getMessage(), e);
                    }
                });
                try {
                    lastModified.put(pathToKey(file), Files.getLastModifiedTime(file).toInstant());
                } catch (IOException e) {
                    log.warn(e.getMessage(), e);
                }
            });
        }

        Callable<Void> callable = () -> {this.watchLoop(); return null;};
        return executorService.submit(callable);
    }

    /**
     * An event in the watched directory
     * @param resolved The resolved path of the event (which may be the target of a symlink)
     *                 The relevant file will always be the key of a map.
     * @param type The type of event
     * @param instant Associated time.
     */
    public record WatcherEvent(
        @NonNull Path file,
        @NonNull Path resolved,
        @With @NonNull WatcherEventType type,
        @NonNull Instant instant) {


    }


    private Instant instant(Path file1, Path file2) throws IOException {
        Instant prop = null;
        if (Files.exists(file1)) {
            prop = Files.getLastModifiedTime(file1).toInstant();
        }
        if (Files.exists(file2)) {
            Instant prop2 = Files.getLastModifiedTime(file2).toInstant();
            if (prop == null || prop2.isAfter(prop)) {
                prop = prop2;
            }
        }
        return prop != null ? prop : clock.instant();
    }

    public enum WatcherEventType {
        CREATE,
        MODIFY,
        DELETE,
        RELINKED;

        public static WatcherEventType of(WatchEvent.Kind<?> type) {
            if (type ==  ENTRY_CREATE) {
                return CREATE;
            } else if (type == ENTRY_MODIFY) {
                return MODIFY;
            } else if (type == ENTRY_DELETE) {
                return DELETE;
            } else {
                return null;
            }
        }
    }

    private void watchLoop() {
        log.info("Watching {}", directory);
        Thread.currentThread().setName("Watcher for " + directory);
        while (true) {
            try {
                final WatchKey key = watcher.take();
                // collect events, so we could report them all at once, comparing lastModified
                final List<WatcherEvent> events = new ArrayList<>();
                try {
                    if (key.watchable() instanceof Path d) {
                        for (final WatchEvent<?> event : key.pollEvents()) {
                            if (event.count() > 1) {
                                log.info("Repeated event {} {}", event.kind(), event.context());
                                continue;
                            }
                            if (event.kind() == null){
                                log.info("Null kind event {}", event.context());
                                continue;
                            }
                            if (event.context() instanceof Path eventPath) {
                                Path file = d.resolve(eventPath);
                                if (d.equals(directory)) {
                                    log.debug("{}", file);
                                    if (filter.test(file)) {
                                        var watcherEvent = new WatcherEvent(
                                            file, file, WatcherEventType.of(event.kind()), instant(file, file));
                                        log.debug("{} {}", event.kind(), event.context());
                                        events.add(watcherEvent);
                                        checkSymlink(file, event.kind());
                                        if (event.kind() == ENTRY_DELETE) {
                                            lastModified.remove(pathToKey(file));

                                        }
                                    } else {
                                        log.debug("Ignored {} {}", event.kind(), event.context());
                                    }
                                } else {
                                    Path resolved = watchedTargetFiles.get(pathToKey(file));
                                    if (resolved != null) {
                                        events.add(new WatcherEvent(file, resolved, WatcherEventType.of(event.kind()), instant(file, resolved)));
                                        if (event.kind() == ENTRY_DELETE) {
                                            lastModified.remove(pathToKey(file));
                                        }
                                    } else {
                                        log.debug("Not a watched file {}", resolved);
                                    }
                                }
                            } else {
                                log.info("Context not a path {}", event.context());
                            }
                        }
                    } else {
                        log.info("Watchable not a path {}", key.watchable());
                    }


                    for (WatcherEvent e : events) {
                        final Instant previous = lastModified.getOrDefault(pathToKey(e.resolved), Instant.EPOCH);
                        final Instant time = e.instant;
                        if (time.isAfter(previous)) {
                            handleEvent(e);
                        } else {
                            log.debug("Ignoring {} because {} <= {}", e, time, previous);
                        }
                    }

                } catch (IOException e) {
                    log.warn(e.getMessage(), e);
                } finally {
                    key.reset();
                }
            } catch (InterruptedException e) {
                log.info("Interrupted watcher for " + directory);
                Thread.currentThread().interrupt();
                break;
            }
        }
    }

    private void handleEvent(WatcherEvent e) {
       counter.incrementAndGet();
        last = clock.instant();
        consumer.accept(e);
        if (e.type != WatcherEventType.DELETE) {
            lastModified.put(pathToKey(e.resolved), e.instant);
        }
    }

    @SneakyThrows
    private Optional<Path> checkSymlink(Path file, WatchEvent.Kind<?> kind) {
        if (kind == ENTRY_DELETE) {
            if (watchedTargetFiles.containsValue(file)) {
                if (watchedTargetFiles.entrySet().removeIf(e -> e.getValue().equals(file))) {
                    log.info("Removed source {}", file);
                }
            }
            // if it was a symlink, then stop watching the target too.
            Path target = watchedTargetFiles.remove(pathToKey(file));
            if (target != null) {
                log.info("Removed target {}", target);
            }
            if (lastModified.remove(pathToKey(file)) != null) {
                log.info("Removed lastModified {}", file);

            }
        }
        if (Files.isSymbolicLink(file)) {
            Path resolve = Files.readSymbolicLink(file);
            if (! resolve.isAbsolute()) {
                resolve = file.getParent().resolve(resolve);
            }
            watchedTargetFiles.entrySet().removeIf(e -> e.getValue().equals(file));
            watchedTargetFiles.put(pathToKey(resolve), file);

            if (!watchedTargetDirectories.contains(pathToKey(resolve.getParent()))) {
                register(resolve.getParent(), watcher);
                watchedTargetDirectories.add(pathToKey(resolve.getParent()));
            }
            return Optional.of(resolve);
        } else {
            return Optional.empty();
        }
    }

    @Override
    public void close() {
        future.cancel(true);
    }

    public Map<String, Path> getWatchedTargetFiles() {
        return Collections.unmodifiableMap(watchedTargetFiles);
    }

    public Set<String> getWatchedTargetDirectories() {
        return Collections.unmodifiableSet(watchedTargetDirectories);
    }

    public Map<String, Instant> getWatchedLastModifieds() {
        return Collections.unmodifiableMap(lastModified);
    }


    public class Admin implements AdminMXBean {
        @Override
        public int getWatchedTargetFiles() {
            return watchedTargetFiles.size();
        }

        @Override
        public int getWatchedTargetDirectories() {
            return watchedTargetDirectories.size();
        }

        @Override
        public int getCount() {
            return counter.get();
        }

        @Override
        public String lastEvent() {
            return String.valueOf(last);
        }
    }

    public interface AdminMXBean {

        int getWatchedTargetFiles();

        int getWatchedTargetDirectories();

        int getCount();

        String lastEvent();

    }

    public static String pathToKey(Path path) {
        return path.toAbsolutePath().toString();
    }

}
