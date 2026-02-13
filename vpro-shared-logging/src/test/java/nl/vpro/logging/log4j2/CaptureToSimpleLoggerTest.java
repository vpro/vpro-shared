package nl.vpro.logging.log4j2;

import lombok.extern.log4j.Log4j2;

import java.util.*;
import java.util.concurrent.*;

import org.junit.jupiter.api.Test;

import nl.vpro.logging.simple.*;

import static org.assertj.core.api.Assertions.assertThat;

@Log4j2

class CaptureToSimpleLoggerTest {
    static final ExecutorService service = Executors.newCachedThreadPool();

    @Test
    public void log() {
        List<Future<?>> futures = new ArrayList<>();
        List<String> messages = Collections.synchronizedList(new ArrayList<>());
        EventSimpleLogger<Event> logger = EventSimpleLogger.of(e -> {
            messages.add(e.getMessage().toString());
        });


        for (int i = 0; i < 10; i++) {
            final int j = i;
            futures.add(service.submit(() -> {
                try (CaptureToSimpleLogger capture = CaptureToSimpleLogger.of(logger)) {

                    log.info("foo" + j);
                    log.info("bar" + j);
                }
                log.info("ready!");
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

        assertThat(messages).containsExactlyInAnyOrder(
            "foo7",
            "foo1",
            "bar7",
            "foo0",
            "bar0",
            "foo8",
            "foo9",
            "foo4",
            "bar4",
            "bar8",
            "foo6",
            "bar9",
            "bar1",
            "bar6",
            "foo2",
            "bar2",
            "foo5",
            "bar5",
            "foo3",
            "bar3"
        );
    }

}
