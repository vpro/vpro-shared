package nl.vpro.util;

import java.io.IOException;
import java.io.InputStream;

import static nl.vpro.util.FileCachingInputStream.EOF;


/**
 * Wraps an {@link InputStream} to limit the number of bytes to produces.
 * <p>
 * Never closes the underlying stream.
 * @author Michiel Meeuwissen
 * @since 3.4
 */
public class InputStreamChunk extends InputStream implements Counted {

    private final InputStream wrapped;
    private long count = 0;
    private final int chunkSize;

    public InputStreamChunk(int chunkSize, InputStream wrapped) {
        this.chunkSize = chunkSize;
        this.wrapped = wrapped;
    }

    @Override
    public int read() throws IOException {
        if (count >= chunkSize) {
            return EOF;
        }
        int result =  wrapped.read();
        if (result != EOF) {
            count++;
        }
        return result;
    }

    @Override
    public int read(byte[] b) throws IOException {
        int maxRead = Math.min(b.length, chunkSize - (int) count);
        if (maxRead == 0) {
            return EOF;
        }
        int result =  wrapped.read(b, 0, maxRead);
        if (result != EOF) {
            count += result;
        }
        return result;
    }


    @Override
    public int read(byte[] b, int off, int len) throws IOException {
        int maxRead = Math.max(0, Math.min(off + len, chunkSize - (int) count) - off);
        if (maxRead == 0) {
            return EOF;
        }
        int result = wrapped.read(b, off, maxRead);
        if (result != EOF) {
            count += result;
        }
        return result;
    }

    @Override
    public Long getCount() {
        return count;
    }

    @Override
    public int available() throws IOException {
        return Math.min(wrapped.available(), chunkSize);
    }
    @Override
    public void close() {
        // nothing to do
    }
}
