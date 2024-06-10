package nl.vpro.util;

import lombok.NonNull;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.stream.Stream;

import org.checkerframework.checker.nullness.qual.Nullable;


/**
 * An utility to harnass {@link WatchService} to watch a directory for set of files. Supporting symlinks and those kind of things.
 *
 * Targeted at watching openshift config maps.
 */
@Slf4j
public class DirectoryWatcher implements AutoCloseable {

    protected final ScheduledExecutorService executorService = Executors.newScheduledThreadPool(1);

    private final Path directory;
    final WatchService watcher;

    private final Consumer<Path> consumer;

    private final Predicate<Path> filter;

    private final Future<?> future;

    final Set<Path> watchedTargetDirectories = new HashSet<>();
    final Map<Path, Path> watchedTargetFiles = new HashMap<>();



    public DirectoryWatcher(@NonNull Path directory, @NonNull Consumer<Path> consumer, @Nullable Predicate<Path> filter) throws IOException {
        this.directory = directory;
        this.watcher = directory.getFileSystem().newWatchService();

        this.consumer = consumer;
        this.filter = filter == null ? (p) -> true : filter;
        this.future = useWatchService();

    }

    private WatchKey register(Path path, WatchService watcher) throws IOException {
        return path.register(watcher, StandardWatchEventKinds.ENTRY_CREATE, StandardWatchEventKinds.ENTRY_MODIFY, StandardWatchEventKinds.ENTRY_DELETE);
    }

    @SneakyThrows
    private Future<?> useWatchService() {
        register(directory, watcher);

        try (Stream<Path> p = Files.list(directory).filter(filter)) {
            p.forEach(file -> checkSymlink(file, StandardWatchEventKinds.ENTRY_CREATE));
        }

        Callable<Void> callable = () -> {
            log.info("Watching {}", directory);
            Thread.currentThread().setName("Watcher for " + directory);
            while (true) {
                try {
                    WatchKey key = watcher.take();
                    List<Path> events = new ArrayList<>();
                    try {
                        if (key.watchable() instanceof Path d) {
                            for (WatchEvent<?> event : key.pollEvents()) {
                                if (event.count() > 1) {
                                    log.info("Repeated event {}", event);
                                    continue;
                                }
                                if (event.context() instanceof Path eventPath) {
                                    Path file = d.resolve(eventPath);
                                    if (d.equals(directory)) {
                                        log.info("{}", file);
                                        if (filter.test(file)) {
                                            log.info(event.kind() + " " + event.context());
                                            checkSymlink(file, event.kind());
                                            events.add(file);
                                        } else {
                                            log.info("Ignored {} {}", event.kind(), event.context());
                                        }
                                    } else {
                                        Path resolved = watchedTargetFiles.get(file);
                                        if (resolved != null) {
                                            events.add(resolved);
                                        } else {
                                            log.info("Not a watched file {}", file);
                                        }
                                    }
                                } else {
                                    log.info("Not a path {}", event.context());
                                }
                            }
                        } else {
                            log.info("Not a path {}", key.watchable());
                        }
                        for (Path p : events) {
                            consumer.accept(p);
                        }

                    } finally {
                        key.reset();
                    }
                } catch (InterruptedException e) {
                    log.info("Interrupted watcher for " + directory);
                    Thread.currentThread().interrupt();
                    break;
                }
            }
            return null;
        };
        return executorService.submit(callable);
    }

    @SneakyThrows
    private void checkSymlink(Path file, WatchEvent.Kind<?> kind) {
        if (kind == StandardWatchEventKinds.ENTRY_DELETE) {
            if (watchedTargetFiles.containsValue(file)) {
                if (watchedTargetFiles.entrySet().removeIf(e -> e.getValue().equals(file))) {
                    log.info("Removed {}", file);
                }
            }
        }

        if (Files.isSymbolicLink(file)) {
            Path resolve = Files.readSymbolicLink(file);
            if (! resolve.isAbsolute()) {
                resolve = file.getParent().resolve(resolve);
            }
            watchedTargetFiles.put(resolve, file);
            if (!watchedTargetDirectories.contains(resolve.getParent())) {
                register(resolve.getParent(), watcher);
                watchedTargetDirectories.add(resolve.getParent());
            }
        }
    }

    @Override
    public void close() {
        future.cancel(true);
    }

    public Map<Path, Path> getWatchedTargetFiles() {
        return Collections.unmodifiableMap(watchedTargetFiles);
    }

    public Set<Path> getWatchedTargetDirectories() {
        return Collections.unmodifiableSet(watchedTargetDirectories);
    }

}
