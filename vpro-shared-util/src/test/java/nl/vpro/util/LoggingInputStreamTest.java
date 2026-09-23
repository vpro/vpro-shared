package nl.vpro.util;

import java.io.IOException;
import java.io.InputStream;
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

    @Test
    void reportsTruncationWhenLaterReadsExceedTheLimit() throws IOException {
        Queue<Event> queue = new ArrayDeque<>();
        QueueSimpleLogger<Event> simpleLogger = QueueSimpleLogger.of(queue);
        try (LoggingInputStream input = new LoggingInputStream(
            simpleLogger,
            new ChunkedInputStream("abcdef".getBytes())
        )) {
            input.setTruncateAfter(4);
            input.readAllBytes();
        }

        assertThat(queue.poll().getMessage().toString())
            .startsWith("body of 6 bytes (truncated):");
    }

    private static class ChunkedInputStream extends InputStream {
        private final byte[] bytes;
        private int position;

        private ChunkedInputStream(byte[] bytes) {
            this.bytes = bytes;
        }

        @Override
        public int read() {
            return position < bytes.length ? bytes[position++] : -1;
        }

        @Override
        public int read(byte[] buffer, int offset, int length) {
            if (position == bytes.length) {
                return -1;
            }
            int read = Math.min(2, Math.min(length, bytes.length - position));
            System.arraycopy(bytes, position, buffer, offset, read);
            position += read;
            return read;
        }
    }

}
