package nl.vpro.util;

import java.io.IOException;
import java.util.ArrayDeque;
import java.util.Queue;

import org.apache.commons.io.IOUtils;
import org.apache.commons.io.output.NullOutputStream;
import org.junit.jupiter.api.Test;

import nl.vpro.logging.simple.Event;
import nl.vpro.logging.simple.QueueSimpleLogger;

import static org.assertj.core.api.Assertions.assertThat;

class LoggingInputStreamTest {

    @Test
    public void test() throws IOException {
        Queue<Event> queue = new ArrayDeque<>();
        int number =  1024 * 1024 * 1024;
        QueueSimpleLogger<Event> simpleLogger = QueueSimpleLogger.of(queue);
        try (RandomStream random = new RandomStream(1, number);
             LoggingInputStream impl = new LoggingInputStream(simpleLogger, random)
        ) {
            impl.setTruncateAfter(2000);
            long result = IOUtils.copyLarge(
                impl,
                NullOutputStream.INSTANCE);
            assertThat(result).isEqualTo(number);
            assertThat(impl.getBytes().toByteArray()).hasSize(2000);
        }
        assertThat(queue).hasSize(1);
        Event event = queue.poll();
        assertThat(event.getMessage().toString()).startsWith("body of 1073741824 bytes (truncated):");


    }

}
