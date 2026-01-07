package nl.vpro.logging.log4j2;

import lombok.extern.log4j.Log4j2;

import java.util.*;
import java.util.concurrent.*;

import org.apache.logging.log4j.Level;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@Log4j2

class CaptureListFromLoggerTest {
    ExecutorService service = Executors.newCachedThreadPool();

    @Test
    public void log() {
        List<Future<?>> futures = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            final int j = i;
            futures.add(service.submit(() -> {
                try (CaptureListFromLogger capture = new CaptureListFromLogger(UUID.randomUUID(), true)) {
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
        service.shutdown();


    }

}
