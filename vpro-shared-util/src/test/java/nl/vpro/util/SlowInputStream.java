package nl.vpro.util;

import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.io.InputStream;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;

@SuppressWarnings("ConstantConditions")
@Slf4j
class SlowInputStream extends InputStream {

    final Random random = new Random();
    final byte[] bytes;
    final AtomicInteger count = new AtomicInteger(0);
    final int sleep;

    SlowInputStream(int sleep, byte[] bytes) {
        this.sleep = sleep;
        this.bytes = bytes;
    }

    @Override
    public int read(byte[] b, int off, int len) throws IOException {
        if (random.nextInt(10) % 10 == 0) {
            // sometimes just read nothing.
            return 0;
        }
        int read = super.read(b, off, len);
        log.debug("Read {}", read);
        return read;
    }

    @Override
    @SneakyThrows
    public int read() {
        Thread.sleep(sleep);
        if (count.get() > bytes.length) {
            log.info("End of stream at {}", count);
            return -1;
        } else {
            int b = Byte.toUnsignedInt(bytes[count.getAndIncrement()]);
            assert b != -1;
            return b;
        }
    }

    @Override
    public String toString() {
        return "Sleepy input stream of " + bytes.length + " bytes";
    }


}
