package nl.vpro.util;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

/**
 * @author Michiel Meeuwissen
 * @since 2.5
 */
public class SkipAtStartInputStream extends InputStream {


    public static final int[] UTF8_BYTE_ORDER_MARK =  {0xEF, 0xBB, 0xBF};
    public static final int[] UTF16_BYTE_ORDER_MARK_BE =  {0xFE, 0xFF};
    public static final int[] UTF16_BYTE_ORDER_MARK_LE =  {0xFF, 0xFE};

    int count = 0;

    final List<Integer> buffer = new ArrayList<>();

    final List<int[]> skip;

    final InputStream wrapped;

    public SkipAtStartInputStream(InputStream wrapped, int[]... skip) {
        this.wrapped = wrapped;
        this.skip = new ArrayList<>(Arrays.asList(skip));
    }

    public static SkipAtStartInputStream skipUnicodeByteOrderMarks(InputStream inputStream) {
        return new SkipAtStartInputStream(inputStream, UTF8_BYTE_ORDER_MARK, UTF16_BYTE_ORDER_MARK_BE, UTF16_BYTE_ORDER_MARK_LE);
    }


    @Override
    public int read() throws IOException {
        if (! buffer.isEmpty()) {
            // some bytes were buffered, but in the end no byte sequence matched. Serve out those first.
            return buffer.remove(0);
        }
        int result = wrapped.read();
        count++;
        if (skip.isEmpty()) {
            return result;
        } else {
            buffer.add(result);
            boolean matching = true;
            while(matching) {
                Iterator<int[]> si = skip.iterator();
                if (! si.hasNext()) {
                    break;
                }
                matching = false;
                while(si.hasNext()) {
                    int[] s = si.next();
                    if (count > s.length) {
                        // we're past the lenght of the array, the array still matches, we're still iterating because longer ones may match too.
                        continue;
                    }
                    if (s[count - 1] == result) {// this array still matches
                        matching = true;
                    } else {
                        // not matched any more, this array certainly does not match.
                        si.remove();
                    }
                }

                if (skip.isEmpty() || ! matching) { // non matched, serve the buffer.
                    break;
                } else {
                    // try the next byte.
                    result = wrapped.read();
                    buffer.add(result);
                    count++;
                }
            }

            if (!skip.isEmpty()) {
                // one or more array matched. So we don't serve the buffer.
                buffer.clear();
                // and we are ready skipping.
                skip.clear();
                return result;
            } else {
                // we serve the buffer
                return buffer.remove(0);
            }

        }


    }

    @Override
    public int read(byte b[]) throws IOException {
        if (skip.isEmpty()) {
            return wrapped.read(b, 0, b.length);
        } else {
            return super.read(b);
        }
    }

    @Override
    public int read(byte b[], int off, int len) throws IOException {
        if (skip.isEmpty()) {
            return wrapped.read(b, off, len);
        } else {
            return super.read(b, off, len);
        }
    }
    @Override
    public long skip(long n) throws IOException {
          if (skip.isEmpty()) {
            return wrapped.skip(n);
        } else {
            return super.skip(n);
        }
    }

    @Override
    public int available() throws IOException {
         if (skip.isEmpty()) {
            return wrapped.available();
        } else {
            return super.available();
        }
    }

    @Override
    public void close() throws IOException {
        wrapped.close();
    }

    @Override
    public synchronized void mark(int readlimit) {
        if (skip.isEmpty()) {
            wrapped.mark(readlimit);
        } else {
            super.mark(readlimit);
        }

    }

    @Override
    public synchronized void reset() throws IOException {
        if (skip.isEmpty()) {
            wrapped.reset();
        } else {
            super.reset();
        }
    }

    @Override
    public boolean markSupported() {
         if (skip.isEmpty()) {
            return wrapped.markSupported();
        } else {
             return super.markSupported();
        }
    }

}
