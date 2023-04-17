package nl.vpro.util;

import java.io.InputStream;
import java.util.Random;

import org.checkerframework.checker.nullness.qual.NonNull;

import static org.apache.commons.io.IOUtils.EOF;

public class RandomStream extends InputStream {
    final Random random;
    final int seed;
    final int size;

    public RandomStream(int seed, int size) {
        this.random = new Random(seed);
        this.seed = seed;
        this.size = size;

    }

    int count = 0;
    @Override
    public int read() {
        if (++count > size) {
            return EOF;
        } else {
            return random.nextInt(256);
        }
    }
    @Override
    public int read(byte @NonNull [] b, int off, int len) {
        if (count >= size) {
            return EOF;
        }
        int l = Math.min(len, size - count);

        nextBytes(random, count, b, off, l);
        count += l;
        return l;
    }
    @Override
    public String toString() {
        return size + " random bytes (seed: " + seed + ")";
    }

    protected void nextBytes(Random random, int count,  byte[] b, int off, int l) {
        if (off == 0 && l == b.length) {
            random.nextBytes(b);
        } else {
            byte[] bytes = new byte[l];
            random.nextBytes(bytes);
            System.arraycopy(bytes, 0, b, off, l);
        }
    }

}
