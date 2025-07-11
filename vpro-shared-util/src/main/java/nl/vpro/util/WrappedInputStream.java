package nl.vpro.util;

import java.io.IOException;
import java.io.InputStream;

import org.checkerframework.checker.nullness.qual.NonNull;
import org.meeuw.functional.Unwrappable;

/**
 * @author Michiel Meeuwissen
 * @since 0.29
 */
public class WrappedInputStream  extends InputStream implements Unwrappable<InputStream> {

    private final InputStream wrapped;

    public WrappedInputStream(InputStream wrapped) {
        this.wrapped = wrapped;
    }


    @Override
    public int read() throws IOException {
        return wrapped.read();

    }

    @Override
    public int read(byte @NonNull [] b) throws IOException {
        return wrapped.read(b);
    }


    @Override
    public int read(byte @NonNull [] b, int off, int len) throws IOException {
        return wrapped.read(b, off, len);
    }


    @Override
    public long skip(long n) throws IOException {
        return wrapped.skip(n);
    }


    @Override
    public int available() throws IOException {
        return wrapped.available();
    }

    @Override
    public void close() throws IOException {
        wrapped.close();
    }

    @Override
    public synchronized void mark(int readlimit) {
        wrapped.mark(readlimit);
    }


    @Override
    public synchronized void reset() throws IOException {
        wrapped.reset();
    }

    @Override
    public boolean markSupported() {
        return wrapped.markSupported();
    }

    @Override
    public InputStream unwrap() {
        return wrapped;
    }
}
