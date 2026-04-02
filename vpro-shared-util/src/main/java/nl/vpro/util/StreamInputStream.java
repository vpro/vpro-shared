package nl.vpro.util;

import org.meeuw.functional.ThrowingFunction;

import java.io.IOException;
import java.io.InputStream;
import java.util.Iterator;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Stream;

/**
 * Simple input stream implementation that converts a stream of objects to an input stream.
 * @param <O>
 * @param <E>
 * @since 5.15
 * @author Michiel Meeuwissen
 */
public class StreamInputStream<O, E extends IOException> extends InputStream {
    private final Stream<O> stream;
    private final Iterator<O> iterator;
    private final AtomicLong counter;
    private byte[] buf = null;
    private int pos = 0;
    private boolean finished = false;
    private final ThrowingFunction<O, byte[], E> toByteArray;



    StreamInputStream(Stream<O> stream, AtomicLong counter, ThrowingFunction<O, byte[], E> toByteArray) {
        this.stream = stream;
        this.iterator = stream.sequential().iterator();
        this.counter = counter;
        this.toByteArray = toByteArray;
    }

    @Override
    public int read() throws E {
        if (finished) return -1;
        if (buf == null || pos >= buf.length) {
            if (!fillBuffer()) return -1;
        }
        return buf[pos++] & 0xFF;
    }

    @Override
    public int read(byte[] b, int off, int len) throws E {
        if (finished) return -1;
        if (b == null) throw new NullPointerException();
        if (off < 0 || len < 0 || off + len > b.length) throw new IndexOutOfBoundsException();

        if (buf == null || pos >= buf.length) {
            if (!fillBuffer()) return -1;
        }
        int toCopy = Math.min(len, buf.length - pos);
        System.arraycopy(buf, pos, b, off, toCopy);
        pos += toCopy;
        return toCopy;
    }

    private boolean fillBuffer() throws E {
        if (!iterator.hasNext()) {
            finished = true;
            return false;
        }
        O p = iterator.next();
        buf = toByteArray.apply(p);

        pos = 0;
        counter.getAndIncrement();
        return true;
    }

    @Override
    public void close() throws IOException {
        try {
            stream.close();
        } finally {
            super.close();
        }
    }
}
