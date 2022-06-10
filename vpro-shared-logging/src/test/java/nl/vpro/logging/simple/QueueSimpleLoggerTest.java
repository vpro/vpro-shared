package nl.vpro.logging.simple;

import java.time.Instant;
import java.time.ZoneId;
import java.util.LinkedList;
import java.util.Queue;

import org.junit.jupiter.api.Test;
import org.meeuw.math.TestClock;

import static org.assertj.core.api.Assertions.assertThat;

class QueueSimpleLoggerTest {

    @Test
    void of() {
        Queue<Event> queue = new LinkedList<>();
        SimpleLogger logger = QueueSimpleLogger.of(queue, new TestClock(ZoneId.of("Europe/Amsterdam"), Instant.parse("2022-06-09T19:51:53.674686Z")));
        logger.debug("foo");
        logger.info("bar");
        logger.error("bar", new Exception("bla"));

        assertThat(queue.stream().map(Event::toString)).containsExactly(
            "2022-06-09T19:51:53.674686Z:DEBUG:foo",
            "2022-06-09T19:51:53.674686Z:INFO:bar",
            "2022-06-09T19:51:53.674686Z:ERROR:bar:java.lang.Exception:bla"
        );
    }
}
