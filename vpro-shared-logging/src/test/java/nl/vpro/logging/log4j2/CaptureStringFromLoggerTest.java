package nl.vpro.logging.log4j2;

import lombok.extern.log4j.Log4j2;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

import org.apache.logging.log4j.Level;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@Log4j2

class CaptureStringFromLoggerTest {
    ExecutorService service = Executors.newCachedThreadPool();

    @Test
    public void log() {
        List<Future<?>> futures = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            final int j = i;
            futures.add(service.submit(() -> {
                try (CaptureStringFromLogger capture = new CaptureStringFromLogger("%msg%n", Level.INFO)) {
                    log.info("foo" + j);
                    log.info("bar" + j);

                    assertThat(capture.get()).isEqualTo("foo%d\nbar%d\n", j, j);
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
