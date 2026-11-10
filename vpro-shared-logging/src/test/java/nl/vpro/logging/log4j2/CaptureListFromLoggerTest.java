package nl.vpro.logging.log4j2;

import lombok.extern.log4j.Log4j2;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

import org.apache.logging.log4j.*;
import org.apache.logging.log4j.core.LogEvent;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@Log4j2

class CaptureListFromLoggerTest {
    ExecutorService service = Executors.newCachedThreadPool();

    @AfterEach
    void tearDown() {
        service.shutdown();
    }
    @Test
    void log() {
        List<Future<?>> futures = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            final int j = i;
            futures.add(service.submit(() -> {
                try (CaptureListFromLogger capture = new CaptureListFromLogger(true)) {
                    log.info("foo" + j);
                    log.info("bar" + j);

                    assertThat(capture.getEvents()).hasSize(2);
                    assertThat(capture.getEvents().get(0).getLevel()).isEqualTo(Level.INFO);

                }
            }));
        }
        futures.forEach(f -> {
            try {
                f.get();
            } catch (Exception e) {
                throw new AssertionError(e);
            }
        });
    }

    @Test
    void logWithFilter() {
        try (CaptureListFromLogger capture = CaptureListFromLogger.builder()
            .predicate(le -> le.getMessage().getFormattedMessage().contains("foo"))
            .currentThreadOnly(true)
            .build()) {
            for (int i = 0; i < 10; i++) {
                log.info("foo" + i);
                log.info("bar" + i);
            }

            assertThat(capture.getEvents()).hasSize(10);
        }
    }


    @Test
    void logWithLevel() {
        Logger logger = LogManager.getLogger("foo");

        try (CaptureListFromLogger capture = CaptureListFromLogger.builder()
            .loggerName("foo")
            .level(Level.INFO)
            .currentThreadOnly(true)
            .build()) {
            logger.info("info");
            logger.debug("debug");
            log.info("ignored");

            assertThat(capture.getEvents()).hasSize(1);
        }
    }

    @Test
    public void awaitFirstBlocksUntilAMatchingEventArrivesFromAnotherThread() throws Exception {
        String marker = "boom-" + System.nanoTime();
        try (CaptureListFromLogger capture = CaptureListFromLogger.builder()
            .level(Level.ERROR)
            .currentThreadOnly(false)
            .build()) {
            service.submit(() -> {
                try {
                    Thread.sleep(200);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                log.error(marker);
            });

            LogEvent event = capture.awaitFirst(
                le -> le.getMessage().getFormattedMessage().equals(marker),
                Duration.ofSeconds(5));

            assertThat(event.getMessage().getFormattedMessage()).isEqualTo(marker);
        }
    }

    @Test
    public void awaitFirstThrowsIfNoMatchingEventArrivesInTime() {
        try (CaptureListFromLogger capture = new CaptureListFromLogger(true)) {
            log.info("not an error");

            assertThatThrownBy(() -> capture.awaitFirst(Level.ERROR, Duration.ofMillis(200)))
                .isInstanceOf(AssertionError.class);
        }
    }

}
