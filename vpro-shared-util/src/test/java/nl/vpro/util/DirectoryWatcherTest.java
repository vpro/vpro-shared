package nl.vpro.util;

import lombok.SneakyThrows;
import lombok.extern.log4j.Log4j2;

import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.FileTime;
import java.time.Duration;
import java.time.Instant;
import java.util.*;

import org.checkerframework.checker.nullness.qual.NonNull;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.DisabledOnOs;
import org.junit.jupiter.api.condition.OS;

import static nl.vpro.logging.Log4j2Helper.debugOrInfo;
import static nl.vpro.util.DirectoryWatcher.pathToKey;
import static org.assertj.core.api.Assertions.assertThat;

@Log4j2
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@DisabledOnOs(OS.WINDOWS)
class DirectoryWatcherTest {


    private final Path dir = Files.createTempDirectory(DirectoryWatcherTest.class.getSimpleName());
    private final Path subDir = Files.createDirectory(dir.resolve("subdir"));

    final Path f1 = Files.createFile(dir.resolve("test-1.xml"));
    final Path tarf5 = Files.createFile(subDir.resolve("test-5-target.xml"));
    final Path symf5 = Files.createSymbolicLink(dir.resolve("test-5.xml"), Path.of("subdir/test-5-target.xml"));// relative!
    final Path tarf3 = Files.createFile(subDir.resolve("test-3-target.xml"));
    final Path tarf3_2 = Files.createFile(subDir.resolve("test-3-target-2.xml"));
    Path f2;
    Path symf3;
    Path symf6;

    final Queue<DirectoryWatcher.@NonNull WatcherEvent> events = new ArrayDeque<>();

    DirectoryWatcher watcher;

    DirectoryWatcherTest() throws IOException {
    }

    @BeforeAll
    public void setup() throws IOException {
        watcher = DirectoryWatcher.builder()
            .directory(dir)
            .eventConsumer(
            path -> {
                events.add(path);
                synchronized (dir) {
                    dir.notifyAll();
                }
            })
            .filter(
                path -> path.getFileName().toString().endsWith(".xml")
            ).build();


        assertThat(watcher.getWatchedTargetFiles().keySet()).containsExactlyInAnyOrder(pathToKey(tarf5));
        assertThat(watcher.getWatchedTargetDirectories()).containsExactlyInAnyOrder(pathToKey(subDir));
    }

    @Test
    @Order(2)
    public void waitForNewFileAnNewSymlink() throws IOException, InterruptedException {


        f2 = Files.createFile(dir.resolve("test-2.xml"));
        symf3 = Files.createSymbolicLink(dir.resolve("test-3.xml"), tarf3);

        wait(f2, symf3);

        Thread.sleep(100);
        assertThat(events).isEmpty();
    }

    @Test
    @Order(3)
    public void touchFile() throws IOException, InterruptedException {
        tickAndTouch(f1);

        wait(f1);
    }
    @Test
    @Order(3)
    public void touchTargetOfSymlink() throws IOException, InterruptedException {

        tickAndTouch(tarf3);

        wait(symf3);
    }

    @Test
    @Order(4)
    public void createSymlink() throws IOException, InterruptedException {
        symf6 = Files.createSymbolicLink(dir.resolve("test-6.xml"), tarf5);

        wait(symf6);

        assertThat(events).isEmpty();
        assertThat(watcher.getWatchedTargetFiles().keySet()).containsExactlyInAnyOrder(pathToKey(tarf3), pathToKey(tarf5));
    }

    @Test
    @Order(5)
    public void deleteSymlink() throws IOException, InterruptedException {
        Files.delete(symf3);

        wait(symf3);

        assertThat(watcher.getWatchedTargetFiles().keySet()).containsExactlyInAnyOrder(pathToKey(tarf5));

        // symf3 has been deleted.
        assertThat(watcher.getWatchedLastModifieds().keySet()).containsExactlyInAnyOrder(pathToKey(f1), pathToKey(f2), pathToKey(tarf5), pathToKey(symf6), pathToKey(symf5));


    }

    @Test
    @Order(5)
    public void newDestinationOfSymlink() throws IOException, InterruptedException {
        tick();

        Files.createSymbolicLink(symf3, tarf3_2);

        wait(symf3);

        assertThat(watcher.getWatchedTargetFiles().keySet())
            .containsExactlyInAnyOrder(pathToKey(tarf3_2), pathToKey(tarf5));
    }


    @Test
    @Order(6)
    public void newDestinationOfSymlink2() throws IOException, InterruptedException {
        tick();

        Files.delete(symf3);
        Files.createSymbolicLink(symf3, tarf3);

        //@Disabled("On my machine it give a MODIFY event, on github actions it gives DELETE and a CREATE")
        try {
            wait(symf3);
        } catch (AssertionError ae) {
            log.warn(ae.getMessage());
            events.clear();
        }

        var keys = watcher.getWatchedTargetFiles().keySet();
        assertThat(keys)
            .containsExactlyInAnyOrder(pathToKey(tarf3), pathToKey( tarf5));



    }

    @Test
    @Order(20)
    @Disabled
    public void noSubsequentEvents() throws InterruptedException {

        for (int i = 0; i < 10; i++) {
            log.info("Checking for subsequent events {}", i);
            Thread.sleep(Duration.ofSeconds(1).toMillis());
            assertThat(events).isEmpty();
        }

    }


    @Test
    @Disabled
    public void tryout() throws IOException {
        Path dir = Files.createTempDirectory(DirectoryWatcherTest.class.getSimpleName());

        var w  = dir.getFileSystem().newWatchService();
        WatchKey register = dir.register(w, StandardWatchEventKinds.ENTRY_CREATE, StandardWatchEventKinds.ENTRY_MODIFY, StandardWatchEventKinds.ENTRY_DELETE);

        w.poll();
    }

    private void touch(Path p, Instant now) throws IOException {
        Files.setLastModifiedTime(p, FileTime.from(now));
    }
    private void tickAndTouch(Path p) throws IOException {
        touch(p, tick());
    }

    @SneakyThrows
    private Instant tick() {
        Thread.sleep(1);
        return Instant.now();

    }

    private void wait(Path... paths) throws InterruptedException {
        int size = paths.length;
        final long start = System.currentTimeMillis();
        log.info("Waiting for {}", Arrays.asList(paths));
        synchronized (dir) {
            int prevSize = 0;
            Instant prevLog = Instant.EPOCH;
            while (events.size() < size && (System.currentTimeMillis() - start) < Duration.ofSeconds(1000).toMillis()) {
                boolean info =  prevSize != events.size() ||
                    Duration.between(prevLog, Instant.now()).compareTo(Duration.ofSeconds(5)) > 0;
                debugOrInfo(log, info, "Waiting for " + size + " events, got " + events.size() + " " + events);
                prevSize = events.size();
                if (info) {
                    prevLog = Instant.now();
                }
                dir.wait(100);
            }
        }
        if (events.size() != size) {
            throw new AssertionError("Expected " + size + " events, but got " + events.size() + "( " + Duration.ofMillis(System.currentTimeMillis() - start) + "):" + events);
        }

        for (Path p : paths) {
            log.info("Waiting for " + p);
            assertThat(events.poll().resolved()).isEqualTo(p);
        }
    }

}
