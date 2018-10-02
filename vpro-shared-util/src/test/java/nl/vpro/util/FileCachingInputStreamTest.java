package nl.vpro.util;

import lombok.extern.slf4j.Slf4j;

import java.io.*;
import java.nio.channels.ClosedByInterruptException;
import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.atomic.AtomicLong;

import org.apache.commons.io.IOUtils;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;

import static org.assertj.core.api.Assertions.assertThat;


/**
 * @author Michiel Meeuwissen
 * @since 0.50
 */
@Slf4j
public class FileCachingInputStreamTest {
    private static final byte[] HELLO = new byte[]{'h', 'e', 'l', 'l', 'o', ' ', 'w', 'o', 'r', 'l', 'd', '!'};
    private static byte[] MANY_BYTES;
    {
        int ncopies = 100;
        MANY_BYTES = new byte[HELLO.length * ncopies];
        for (int i = 0; i < ncopies; i++) {
            System.arraycopy(HELLO, 0, MANY_BYTES, i * HELLO.length, HELLO.length);
        }
    }
    @Rule
    public TestName name = new TestName();


    @Before
    public void before() {
        log.info("-----{}. Interrupted {}", name.getMethodName(), Thread.interrupted());
    }

    @Test
    public void testRead() throws IOException {

        FileCachingInputStream inputStream =  slowReader();

        ByteArrayOutputStream out = new ByteArrayOutputStream();

        int r;
        while ((r = inputStream.read()) != -1) {
            out.write(r);
        }

        assertThat(out.toByteArray()).containsExactly(MANY_BYTES);
    }


    @Test
    public void testReadBuffer() throws IOException {

        FileCachingInputStream inputStream = slowReader();

        ByteArrayOutputStream out = new ByteArrayOutputStream();

        int r;
        byte[] buffer = new byte[10];
        while ((r = inputStream.read(buffer)) != -1) {
            out.write(buffer, 0, r);
        }

        assertThat(out.toByteArray()).containsExactly(MANY_BYTES);
    }


    @Test(expected = IOException.class)
    public void testReadFileGetsBroken() throws IOException {
        FileCachingInputStream inputStream = FileCachingInputStream.builder()
            .outputBuffer(2)
            .batchSize(3)
            .batchConsumer((f, c) -> {
                if (c.getCount() > 300) {
                    try {
                        f.close();
                    } catch (IOException e) {
                        log.error(e.getMessage(), e);
                    }

                }
            })
            .input(new ByteArrayInputStream(MANY_BYTES))
            .initialBuffer(4)
            .startImmediately(true)
            .build();

        ByteArrayOutputStream out = new ByteArrayOutputStream();

        int r;
        byte[] buffer = new byte[10];
        while ((r = inputStream.read(buffer)) != -1) {
            out.write(buffer, 0, r);
        }


    }


    @Test
    public void testReadFileGetsInterrupted() throws IOException {
        final Thread thisThread = Thread.currentThread();

        final AtomicLong interrupted = new AtomicLong(0);
        final AtomicLong closed = new AtomicLong(0);
        boolean isInterrupted = false;
        try(
            FileCachingInputStream inputStream = FileCachingInputStream.builder()
                .outputBuffer(2)
                .batchSize(3)
                .batchConsumer((f, c) -> {
                    if (c.getCount() > 300) {
                        long i = interrupted.getAndIncrement();
                        if (closed.get() > 0) {
                            throw new RuntimeException("Called while closed!");
                        }
                        if (! thisThread.isInterrupted())  {
                            log.info("{} Interrupting {} {}", c.getCount(), thisThread, i);
                            thisThread.interrupt();
                            // According to javadoc this will either cause an exception or set the interrupted status.

                        } else {
                            log.info("{} Interrupted already {} {}", c.getCount(), thisThread, i);
                        }
                    }
                })
                .input(new ByteArrayInputStream(MANY_BYTES))
                .initialBuffer(4)
                .startImmediately(false)
                .build();
        ) {
            ByteArrayOutputStream out = new ByteArrayOutputStream();

            int r;
            byte[] buffer = new byte[10];
            while ((r = inputStream.read(buffer)) != -1) {
                out.write(buffer, 0, r);
            }
        } catch (ClosedByInterruptException | InterruptedIOException  ie) {
            isInterrupted = true;
            log.info("Catched {}", ie.getClass() + " " + ie.getMessage());
        } finally {
            isInterrupted |= thisThread.isInterrupted();
            closed.getAndIncrement();
            log.info("Finally: interrupted: {}: times: {} ", thisThread.isInterrupted(), interrupted.get());

        }
        assertThat(isInterrupted).withFailMessage("Thread did not get interrupted").isTrue();
    }

    protected FileCachingInputStream slowReader() throws IOException {
        return
        FileCachingInputStream.builder()
            .outputBuffer(2)
            .batchSize(3)
            .batchConsumer((f, c) -> {
                try {
                    //log.info("sleeping");
                    Thread.sleep(5L);
                } catch (InterruptedException e) {
                    log.error(e.getMessage(), e);
                }
                //log.info("count:" + c.getCount());
            })
            .input(new ByteArrayInputStream(MANY_BYTES))
            .initialBuffer(4)
            .startImmediately(true)
            .build();
    }

    @Test
    public void testReadNoAutoStart() throws IOException {
        FileCachingInputStream inputStream = FileCachingInputStream.builder()
            .outputBuffer(2)
            .batchSize(3)
            .input(new ByteArrayInputStream(MANY_BYTES))
            .initialBuffer(4)
            .noProgressLogging()
            .startImmediately(false)
            .build();

        ByteArrayOutputStream out = new ByteArrayOutputStream();

        int r;
        while ((r = inputStream.read()) != -1) {
            out.write(r);
        }

        assertThat(out.toByteArray()).containsExactly(MANY_BYTES);
    }


    @Test
    public void testReadLargeBuffer() throws IOException {

        FileCachingInputStream inputStream = FileCachingInputStream.builder()
            .outputBuffer(2)
            .batchSize(3)
            .input(new ByteArrayInputStream(MANY_BYTES))
            .initialBuffer(1024)
            .noProgressLogging()
            .build();

        ByteArrayOutputStream out = new ByteArrayOutputStream();

        int r;
        while ((r = inputStream.read()) != -1) {
            out.write(r);
        }

        assertThat(out.toByteArray()).containsExactly(MANY_BYTES);
    }

    @Test
    public void testSimple() throws IOException {
        try (
            FileCachingInputStream inputStream = FileCachingInputStream.builder()
                .outputBuffer(2)
                .batchSize(3)
                .input(new ByteArrayInputStream(MANY_BYTES))
                .initialBuffer(4)
                .noProgressLogging()
                .startImmediately(true)
                .build()) {

            ByteArrayOutputStream out = new ByteArrayOutputStream();

            IOUtils.copy(inputStream, out);

            assertThat(out.toByteArray()).containsExactly(MANY_BYTES);
        }
    }

    @Test
    public void testNoAutostart() throws IOException {
        try (
            FileCachingInputStream inputStream = FileCachingInputStream.builder()
                .outputBuffer(2)
                .batchSize(3)
                .input(new ByteArrayInputStream(MANY_BYTES))
                .initialBuffer(4)
                .startImmediately(false)
                .noProgressLogging()
                .build();) {

            ByteArrayOutputStream out = new ByteArrayOutputStream();

            IOUtils.copy(inputStream, out);

            assertThat(out.toByteArray()).containsExactly(MANY_BYTES);
        }
    }

    @Test
    public void testWithLargeBuffer() throws IOException {
        try (
            FileCachingInputStream inputStream = FileCachingInputStream.builder()
                .outputBuffer(2)
                .batchSize(3)
                .input(new ByteArrayInputStream(HELLO))
                .initialBuffer(2048)
                .build();) {


            ByteArrayOutputStream out = new ByteArrayOutputStream();

            IOUtils.copy(inputStream, out);

            assertThat(out.toByteArray()).containsExactly(HELLO);
        }
    }

    @Test
    public void testWithBufferEdge() throws IOException {
        try (
            FileCachingInputStream inputStream = FileCachingInputStream.builder()
                .outputBuffer(2)
                .batchSize(1)
                .input(new ByteArrayInputStream(HELLO))
                .initialBuffer(HELLO.length)
                .build();) {


            ByteArrayOutputStream out = new ByteArrayOutputStream();

            IOUtils.copy(inputStream, out);

            assertThat(out.toByteArray()).containsExactly(HELLO);
        }
    }


    @Test
    public void testWaitForBytes() throws IOException, InterruptedException {
        try (
            FileCachingInputStream inputStream = FileCachingInputStream.builder()
                .outputBuffer(2)
                .batchSize(1)
                .input(new ByteArrayInputStream(HELLO))
                .initialBuffer(4)
                .build();) {

            long count = inputStream.waitForBytesRead(10);
            log.info("Found {}", count);
            assertThat(count).isGreaterThanOrEqualTo(1);

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            IOUtils.copy(inputStream, out);

            assertThat(inputStream.getCount()).isEqualTo(HELLO.length);
            assertThat(out.toByteArray()).containsExactly(HELLO);
        }
    }

    @Test
    public void testWaitForBytesOnZeroBytes() throws IOException, InterruptedException {
        try (
            FileCachingInputStream inputStream = FileCachingInputStream.builder()
                .outputBuffer(2)
                .batchSize(1)
                .input(new ByteArrayInputStream(new byte[0]))
                .initialBuffer(4)
                .build()) {

            long count = inputStream.waitForBytesRead(10);
            log.info("Found {}", count);
            assertThat(count).isEqualTo(0);

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            IOUtils.copy(inputStream, out);

            assertThat(inputStream.getCount()).isEqualTo(0);
            assertThat(out.toByteArray()).hasSize(0);
        }
    }

    @Test
    public void createPath() {
        new File("/tmp/bestaatniet").delete();
        FileCachingInputStream.builder()
            .outputBuffer(2)
            .batchSize(1)
            .tempDir("/tmp/bestaatniet")
            .input(new ByteArrayInputStream(HELLO))
            .initialBuffer(HELLO.length)
            .build();

        assertThat(new File("/tmp/bestaatniet")).exists();

    }

    @Test
    @Ignore
    public void performance() throws IOException {
        Instant now = Instant.now();
        try (
            FileCachingInputStream inputStream = FileCachingInputStream
                .builder()
                .input(new BufferedInputStream(new FileInputStream(new File("/tmp/pageupdates.json"))))
                .batchSize(1000000000L)
                .build();
            OutputStream out = new BufferedOutputStream(new FileOutputStream(new File("/tmp/copy.json")))
        ) {
            IOUtils.copyLarge(inputStream, out);
            System.out.println("Duration " + Duration.between(now, Instant.now()));
        }


    }

    @Test
    @Ignore
    public void verify() throws IOException {
        Instant now = Instant.now();
        try (
            FileCachingInputStream inputStream = FileCachingInputStream
                .builder()
                .input(
                    new DelayedInputStream(
                        new BufferedInputStream(new FileInputStream(new File("/tmp/changes.json"))),
                        Duration.ofMillis(50)))
                .batchSize(1000000000L)
                .build();
            OutputStream out = new BufferedOutputStream(new FileOutputStream(new File("/tmp/copy.json")))
        ) {
            IOUtils.copyLarge(inputStream, out);
            out.close();
            assertThat(IOUtils.contentEquals(new FileInputStream(new File("/tmp/changes.json")), new FileInputStream(new File("/tmp/copy.json")))).isTrue();
            System.out.println("Duration " + Duration.between(now, Instant.now()));
        }


    }

    @Test
    @Ignore
    public void performanceBenchmark() throws IOException {
        Instant now = Instant.now();
        try (
            InputStream inputStream = new BufferedInputStream(new FileInputStream(new File("/tmp/pageupdates.json")));
            OutputStream out = new BufferedOutputStream(new FileOutputStream(new File("/tmp/copy.json")))
        ) {
            IOUtils.copyLarge(inputStream, out);
            System.out.println("Duration " + Duration.between(now, Instant.now()));
        }


    }


    @Test
    @Ignore
    public void testLarge() throws IOException {
        Instant now = Instant.now();
        try (
            FileCachingInputStream inputStream = FileCachingInputStream.builder()
                .input(new FileInputStream(new File("/tmp/test.mp4")))
                .build()
            ;
            OutputStream out = new BufferedOutputStream(new FileOutputStream(new File("/tmp/copy.mp4")))
        ) {
            IOUtils.copyLarge(inputStream, out);
            System.out.println("Duration " + Duration.between(now, Instant.now()));
        }

    }


}
