package nl.vpro.util;

import lombok.extern.log4j.Log4j2;

import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.FileTime;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Queue;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
@Log4j2
class DirectoryWatcherTest {

    private final Path dir = Files.createTempDirectory(DirectoryWatcherTest.class.getSimpleName());
    private final Path subDir = Files.createDirectory(dir.resolve("subdir"));

    final Queue<Path> events = new ArrayDeque<>();

    DirectoryWatcherTest() throws IOException {
    }


    @Test
    public void test() throws IOException {
        Path f1 = Files.createFile(dir.resolve("test-1.xml"));
        Path tarf5 = Files.createFile(subDir.resolve("test-5-target.xml"));
        Path symf5 = Files.createSymbolicLink(dir.resolve("test-5.xml"), tarf5);
        Path tarf3 = Files.createFile(subDir.resolve("test-3-target.xml"));
        Path tarf3_2 = Files.createFile(subDir.resolve("test-3-target-2.xml"));

        try (DirectoryWatcher watcher = new DirectoryWatcher(
            dir,
            path -> {
                events.add(path);
                synchronized (dir) {
                    dir.notifyAll();
                }
            },
            path -> path.getFileName().toString().endsWith(".xml")

        )) {

            assertThat(watcher.getWatchedTargetFiles().keySet()).containsExactlyInAnyOrder(tarf5);
            //assertThat(watcher.getWatchedTargetDirectories()).containsExactlyInAnyOrder(subDir);

            Path f2 = Files.createFile(dir.resolve("test-2.xml"));
            Path symf3 = Files.createSymbolicLink(dir.resolve("test-3.xml"), tarf3);

            wait(f2, symf3);

            Thread.sleep(100);
            assertThat(events).isEmpty();

            Files.setLastModifiedTime(f1, FileTime.from(Instant.now()));

            wait(f1);

            Files.setLastModifiedTime(tarf3, FileTime.from(Instant.now()));

            wait(symf3);


            Path symf6 = Files.createSymbolicLink(dir.resolve("test-6.xml"), tarf5);

            wait(symf6);

            assertThat(events).isEmpty();

            assertThat(watcher.getWatchedTargetFiles().keySet()).containsExactlyInAnyOrder(tarf3, tarf5);

            Files.delete(symf3);

            wait(symf3);

            symf3 = Files.createSymbolicLink(dir.resolve("test-3.xml"), tarf3_2);

            wait(symf3);

            assertThat(watcher.getWatchedTargetFiles().keySet()).containsExactlyInAnyOrder(tarf3_2, tarf5);


            for (int i = 0; i < 20; i++) {
                log.info("Checking for subsequent events {}", i);
                Thread.sleep(Duration.ofSeconds(1).toMillis());
                assertThat(events).isEmpty();
            }

        } catch (InterruptedException e) {
            log.warn(e.getMessage(), e);
        }

    }

    @Test
    public void tryout() throws IOException {
        Path dir = Files.createTempDirectory(DirectoryWatcherTest.class.getSimpleName());

        var w  = dir.getFileSystem().newWatchService();
        WatchKey register = dir.register(w, StandardWatchEventKinds.ENTRY_CREATE, StandardWatchEventKinds.ENTRY_MODIFY, StandardWatchEventKinds.ENTRY_DELETE);

        w.poll();
    }


    private void wait(Path... paths) throws InterruptedException {
        int size = paths.length;
        final long start = System.currentTimeMillis();
        synchronized (dir) {
            while (events.size() < size && (System.currentTimeMillis() - start) < Duration.ofSeconds(1000).toMillis()) {
                dir.wait(100);
            }
        }
        if (events.size() != size) {
            throw new AssertionError("Expected " + size + " events, but got " + events.size() + "( " + Duration.ofMillis(System.currentTimeMillis() - start) + ")");
        }

        for (Path p : paths) {
            assertThat(events.poll()).isEqualTo(p);
        }
    }

}
