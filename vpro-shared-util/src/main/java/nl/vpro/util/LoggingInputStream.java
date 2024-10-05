package nl.vpro.util;

import lombok.Getter;
import lombok.Setter;

import java.io.*;
import java.nio.charset.StandardCharsets;

import org.apache.commons.io.input.ObservableInputStream;

import nl.vpro.logging.simple.SimpleLogger;


/**
 * A wrapper for an {@link InputStream} that logs it's first bytes.
 */
@Setter
@Getter
public class LoggingInputStream  extends ObservableInputStream {

    private int truncateAfter = 2048;
    private final ByteArrayOutputStream bytes = new ByteArrayOutputStream();

    public LoggingInputStream(SimpleLogger log, InputStream wrapped) {
        super(wrapped);
        add(new ObservableInputStream.Observer() {

            private boolean truncated = false;
            private long count = 0;

            @Override
            public void data(final byte[] buffer, final int offset, final int length) {
                if (bytes.size() < truncateAfter) {
                    int effectiveLength = Math.min(truncateAfter - bytes.size(), length);
                    truncated = effectiveLength < length;
                    bytes.write(buffer, offset, effectiveLength);
                }
                count += length;
            }

            @Override
            public void data(final int value) {
                if (bytes.size() < truncateAfter) {
                    bytes.write(value);
                } else {
                    truncated = true;
                }
                count++;
            }

            @Override
            public void closed() throws IOException {
                log.info("body of {} bytes{}:\n{}{}\n", count, truncated ? " (truncated)" : "", bytes.toString(StandardCharsets.UTF_8), truncated ? "..." : "");
            }
        });
    }
}
