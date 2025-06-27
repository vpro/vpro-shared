package nl.vpro.util;

import lombok.Getter;
import lombok.Setter;

import java.io.IOException;
import java.io.InputStream;

import org.apache.commons.io.input.ObservableInputStream;


/**
 * A wrapper for an {@link InputStream} that observes its first bytes.
 * @since 5.5
 * @author Michiel Meeuwissen
 */
@Setter
@Getter
public abstract class TruncatedObservableInputStream extends ObservableInputStream {

    private int truncateAfter = 2048;
    protected TruncatedObservableInputStream(InputStream wrapped) {
        super(wrapped);
        add(new Observer() {
            private boolean truncated = false;
            private long count = 0;

            @Override
            public void data(final byte[] buffer, final int offset, final int length) throws IOException{
                if (count < truncateAfter) {
                    int effectiveLength = Math.min(truncateAfter - (int) count, length);
                    truncated = effectiveLength < length;
                    write(buffer, offset, effectiveLength);
                }
                count += length;
            }

            @Override
            public void data(final int value) throws IOException{
                if (count < truncateAfter) {
                    write(value);
                } else {
                    truncated = true;
                }
                count++;
            }

            @Override
            public void closed() throws IOException {
                TruncatedObservableInputStream.this.closed(count, true);
            }
        });
    }

    abstract void write(byte[] buffer, int offset, int length) throws IOException;
    abstract void write(int value) throws IOException;

    void closed(long count, boolean truncated) throws IOException {

    }
}
