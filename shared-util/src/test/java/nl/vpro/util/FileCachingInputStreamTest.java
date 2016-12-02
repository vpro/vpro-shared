package nl.vpro.util;

import org.apache.commons.io.IOUtils;
import org.junit.Ignore;
import org.junit.Test;

import java.io.*;
import java.time.Duration;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;


/**
 * @author Michiel Meeuwissen
 * @since 0.50
 */
public class FileCachingInputStreamTest {
    byte[] in = new byte[]{'h', 'e', 'l', 'l', 'o', ' ', 'w', 'o', 'r', 'l', 'd'};


    @Test
    public void testRead() throws IOException {
        FileCachingInputStream inputStream = FileCachingInputStream.builder()
            .outputBuffer(2)
            .batchSize(3)
            .input(new ByteArrayInputStream(in))
            .initialBuffer(4)
            .startImmediately(true)
            .build();

        ByteArrayOutputStream out = new ByteArrayOutputStream();

        int r;
        while ((r = inputStream.read()) != -1) {
            out.write(r);
        }

        assertThat(out.toByteArray()).containsExactly(in);
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
                .build();) {

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
                .input(new ByteArrayInputStream(in))
                .initialBuffer(2048)
                .build();) {


            ByteArrayOutputStream out = new ByteArrayOutputStream();

            IOUtils.copy(inputStream, out);

            assertThat(out.toByteArray()).containsExactly(in);
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
                .input(new DelayedInputStream(new BufferedInputStream(new FileInputStream(new File("/tmp/changes.json"))), 50L))
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
