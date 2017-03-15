package nl.vpro.util;

import lombok.extern.slf4j.Slf4j;

import java.io.*;
import java.nio.channels.ClosedByInterruptException;
import java.time.Duration;
import java.time.Instant;

import org.apache.commons.io.IOUtils;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;


/**
 * @author Michiel Meeuwissen
 * @since 0.50
 */
@Slf4j
public class FileCachingInputStreamTest {
    byte[] hello = new byte[]{'h', 'e', 'l', 'l', 'o', ' ', 'w', 'o', 'r', 'l', 'd', '!'};
    byte[] in;
    {
        int ncopies = 100;
        in = new byte[hello.length * ncopies];
        for (int i = 0; i < ncopies; i++) {
            System.arraycopy(hello, 0, in, i * hello.length, hello.length);
        }
    }

    @Before
    public void before() {
        Thread.interrupted();
    }

    @Test
    public void testRead() throws IOException {

        FileCachingInputStream inputStream =  slowReader();

        ByteArrayOutputStream out = new ByteArrayOutputStream();

        int r;
        while ((r = inputStream.read()) != -1) {
            out.write(r);
        }

        assertThat(out.toByteArray()).containsExactly(in);
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

        assertThat(out.toByteArray()).containsExactly(in);
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
            .input(new ByteArrayInputStream(in))
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


    @Test(expected = ClosedByInterruptException.class)
    public void testReadFileGetsInterrupted() throws IOException {
        Thread thisThread = Thread.currentThread();
        try {
            FileCachingInputStream inputStream = FileCachingInputStream.builder()
                .outputBuffer(2)
                .batchSize(3)
                .batchConsumer((f, c) -> {
                    if (c.getCount() > 300) {
                        log.info("Interrupting");
                        thisThread.interrupt();
                    }
                })
                .input(new ByteArrayInputStream(in))
                .initialBuffer(4)
                .startImmediately(true)
                .build();

            ByteArrayOutputStream out = new ByteArrayOutputStream();

            int r;
            byte[] buffer = new byte[10];
            while ((r = inputStream.read(buffer)) != -1) {
                out.write(buffer, 0, r);
            }
        } catch (ClosedByInterruptException ie) {
            throw ie;
        } finally {
            Thread.interrupted();
        }
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
            .input(new ByteArrayInputStream(in))
            .initialBuffer(4)
            .startImmediately(true)
            .build();
    }

    @Test
    public void testReadNoAutoStart() throws IOException {
        FileCachingInputStream inputStream = FileCachingInputStream.builder()
            .outputBuffer(2)
            .batchSize(3)
            .input(new ByteArrayInputStream(in))
            .initialBuffer(4)
            .startImmediately(false)
            .build();

        ByteArrayOutputStream out = new ByteArrayOutputStream();

        int r;
        while ((r = inputStream.read()) != -1) {
            out.write(r);
        }

        assertThat(out.toByteArray()).containsExactly(in);
    }


    @Test
    public void testReadLargeBuffer() throws IOException {

        FileCachingInputStream inputStream = FileCachingInputStream.builder()
            .outputBuffer(2)
            .batchSize(3)
            .input(new ByteArrayInputStream(in))
            .initialBuffer(1024)
            .build();

        ByteArrayOutputStream out = new ByteArrayOutputStream();

        int r;
        while ((r = inputStream.read()) != -1) {
            out.write(r);
        }

        assertThat(out.toByteArray()).containsExactly(in);
    }

    @Test
    public void test() throws IOException {
        try (
            FileCachingInputStream inputStream = FileCachingInputStream.builder()
                .outputBuffer(2)
                .batchSize(3)
                .input(new ByteArrayInputStream(in))
                .initialBuffer(4)
                .startImmediately(true)
                .build()) {

            ByteArrayOutputStream out = new ByteArrayOutputStream();

            IOUtils.copy(inputStream, out);

            assertThat(out.toByteArray()).containsExactly(in);
        }
    }

    @Test
    public void testNoAutostart() throws IOException {
        try (
            FileCachingInputStream inputStream = FileCachingInputStream.builder()
                .outputBuffer(2)
                .batchSize(3)
                .input(new ByteArrayInputStream(in))
                .initialBuffer(4)
                .startImmediately(false)
                .build();) {

            ByteArrayOutputStream out = new ByteArrayOutputStream();

            IOUtils.copy(inputStream, out);

            assertThat(out.toByteArray()).containsExactly(in);
        }
    }

    @Test
    public void testWithLargeBuffer() throws IOException {
        try (
            FileCachingInputStream inputStream = FileCachingInputStream.builder()
                .outputBuffer(2)
                .batchSize(3)
                .input(new ByteArrayInputStream(hello))
                .initialBuffer(2048)
                .build();) {


            ByteArrayOutputStream out = new ByteArrayOutputStream();

            IOUtils.copy(inputStream, out);

            assertThat(out.toByteArray()).containsExactly(hello);
        }
    }

    @Test
    public void testWithBufferEdge() throws IOException {
        try (
            FileCachingInputStream inputStream = FileCachingInputStream.builder()
                .outputBuffer(2)
                .batchSize(1)
                .input(new ByteArrayInputStream(hello))
                .initialBuffer(hello.length)
                .build();) {


            ByteArrayOutputStream out = new ByteArrayOutputStream();

            IOUtils.copy(inputStream, out);

            assertThat(out.toByteArray()).containsExactly(hello);
        }
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


}
