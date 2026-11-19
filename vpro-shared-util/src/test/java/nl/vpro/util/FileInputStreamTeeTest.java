package nl.vpro.util;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class FileInputStreamTeeTest {

    @Test
    void writesReadBytesToOutputStream() throws IOException {
        ByteArrayOutputStream output = new ByteArrayOutputStream();

        try (FileInputStreamTee tee = new FileInputStreamTee(
            output,
            new ByteArrayInputStream("request body".getBytes())
        )) {
            tee.setTruncateAfter(7);
            tee.readAllBytes();
        }

        assertThat(output).hasToString("request");
    }

    @Test
    void closesOutputStreamWhenWrappedStreamCloseFails() {
        IOException closeException = new IOException("wrapped stream failed to close");
        TrackingOutputStream output = new TrackingOutputStream();
        InputStream input = new ByteArrayInputStream(new byte[0]) {
            @Override
            public void close() throws IOException {
                throw closeException;
            }
        };
        FileInputStreamTee tee = new FileInputStreamTee(output, input);

        assertThatThrownBy(tee::close).isInstanceOf(IOException.class);
        assertThat(output.closed).isTrue();
    }

    private static class TrackingOutputStream extends ByteArrayOutputStream {
        private boolean closed;

        @Override
        public void close() throws IOException {
            closed = true;
            super.close();
        }
    }
}
