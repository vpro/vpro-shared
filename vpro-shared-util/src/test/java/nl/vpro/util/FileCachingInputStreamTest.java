package nl.vpro.util;

import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

import java.io.*;
import java.nio.channels.ClosedByInterruptException;
import java.nio.file.Files;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;

import org.apache.commons.io.IOUtils;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.Isolated;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.opentest4j.AssertionFailedError;

import static nl.vpro.util.FileCachingInputStream.throttle;
import static org.apache.commons.io.IOUtils.EOF;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.parallel.ExecutionMode.SAME_THREAD;


/**
 * @author Michiel Meeuwissen
 * @since 0.50
 */
@Slf4j
@Isolated
@Execution(SAME_THREAD)
@TestMethodOrder(MethodOrderer.DisplayName.class)
public class FileCachingInputStreamTest {
    private static final Random RANDOM = new Random();
    private static final int SIZE_OF_BIG_STREAM = 10_000 + RANDOM.nextInt(1_000);
    private static final int SIZE_OF_HUGE_STREAM = 1_000_000_000 + RANDOM.nextInt(1_000_000);

    private static final int SEED_FOR_LARGE_RANDOM_FILE = RANDOM.nextInt();
    private static final byte[] HELLO = new byte[]{'h', 'e', 'l', 'l', 'o', ' ', 'w', 'o', 'r', 'l', 'd', '!'};
    private static final byte[] MANY_BYTES;
    static {
        MANY_BYTES = new byte[1500];
        RANDOM.nextBytes(MANY_BYTES);
    }



    @SuppressWarnings("OptionalGetWithoutIsPresent")
    @BeforeEach
    public void before(TestInfo testInfo) {
        log.info(">-----{}. Interrupted {}, openstreams: {}", testInfo.getTestMethod().get().getName(), Thread.interrupted(), FileCachingInputStream.openStreams);
        FileCachingInputStream.openStreams.set(0);
    }

    @SuppressWarnings("OptionalGetWithoutIsPresent")
    @AfterEach
    public void after(TestInfo testInfo) {
        boolean wasInterrupted = Thread.interrupted();
        synchronized (FileCachingInputStream.openStreams) {
            int tries = 0;
            while (FileCachingInputStream.openStreams.get() != 0 && tries++ < 10) {
                try {
                    FileCachingInputStream.openStreams.wait(1000);
                } catch (InterruptedException ignored) {

                }
            }
        }
        log.info("<-----{}. Interrupted {}, openstreams: {}", testInfo.getTestMethod().get().getName(), wasInterrupted, FileCachingInputStream.openStreams);
        assertThat(FileCachingInputStream.openStreams.get()).isEqualTo(0);
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    public void testRead(boolean downloadFirst) throws IOException {

        try(FileCachingInputStream inputStream =  slowReader(downloadFirst);
            ByteArrayOutputStream out = new ByteArrayOutputStream()
        ) {
            int r;
            while ((r = inputStream.read()) != -1) {
                out.write(r);
            }

            assertThat(out.toByteArray()).containsExactly(MANY_BYTES);
        }
    }


    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    public void testReadBuffer(boolean downloadFirst) throws IOException {

        try(FileCachingInputStream inputStream = slowReader(downloadFirst)) {

            ByteArrayOutputStream out = new ByteArrayOutputStream();

            int r;
            byte[] buffer = new byte[10];
            while ((r = inputStream.read(buffer, 1, 9)) != -1) {
                out.write(buffer, 1, r);
            }

            assertThat(out.toByteArray()).containsExactly(MANY_BYTES);
        }
    }

    @Test
    public void testReadFileGetsBroken()  {
        assertThatThrownBy(() -> {
            try (
                FileCachingInputStream inputStream = FileCachingInputStream
                    .builder()
                    .outputBuffer(2)
                    .noProgressLogging()
                    .batchSize(3)
                    .downloadFirst(false) // if true exception will come from constructor already
                    .batchConsumer((f) -> {
                        if (f.getCount() > 300) {
                            // randomly close the file
                            // this should be dealt with gracefully
                            if (! f.isClosed()) {
                                log.debug("Closing {}", f);
                                try {
                                    f.closeTempFile();
                                } catch (IOException e) {
                                    log.error(e.getMessage(), e);
                                }
                            }
                        }
                    })
                    .input(new ByteArrayInputStream(MANY_BYTES))

                    .initialBuffer(4)
                    .startImmediately(true)
                    .build()) {

                ByteArrayOutputStream out = new ByteArrayOutputStream();

                int r;
                byte[] buffer = new byte[10];
                while ((r = inputStream.read(buffer)) != -1) {
                    out.write(buffer, 0, r);
                }
            }
        }).isInstanceOf(IOException.class)
            .hasMessageContaining("Stream closed");
    }


    public static Iterator<Object[]> slowAndNormal() {

        return new Iterator<Object[]>() {
            int count = 0;
            @Override
            public boolean hasNext() {
                return count < 100;
            }

            @Override
            public Object[] next() {
                if (count++ < 10) {
                    return new Object[] {new ByteArrayInputStream(MANY_BYTES), (long) MANY_BYTES.length};

                } else {
                    return new Object[] {new InputStream() {
                        int count = -1;
                        @Override
                        @SneakyThrows
                        public int read() {
                            count++;
                            Thread.sleep(5);
                            if (count >= MANY_BYTES.length) {
                                return -1;
                            } else {
                                return MANY_BYTES[count];
                            }
                        }
                        @Override
                        public String toString() {
                            return "Sleepy input stream of " + MANY_BYTES.length + " bytes";
                        }
                    }, (long) MANY_BYTES.length};

                }
            }
        };
    }


    @ParameterizedTest
    @MethodSource("slowAndNormal")
    public void testReadFileGetsInterrupted(InputStream input, Long expectedCount) throws IOException {
        final Thread thisThread = Thread.currentThread();

        final AtomicLong interrupted = new AtomicLong(0);
        final AtomicLong closed = new AtomicLong(0);
        boolean isInterrupted = false;
        try{
            try (
            FileCachingInputStream inputStream = FileCachingInputStream.builder()
                .outputBuffer(2)
                .batchSize(3)
                .batchConsumer((f) -> {
                    log.info("count consumer {} ", f.getCount());
                    if (f.getCount() > 300) {
                        long i = interrupted.getAndIncrement();
                        if (closed.get() > 0) {
                            throw new RuntimeException("Called while closed!");
                        }
                        if (! thisThread.isInterrupted())  {
                            log.info("{} Interrupting {} {}", f.getCount(), thisThread, i);
                            thisThread.interrupt();
                            // According to javadoc this will either cause an exception or set the interrupted status.

                        } else {
                            log.info("{} Interrupted already {} {}", f.getCount(), thisThread, i);
                        }
                    }
                })
                .input(input)
                .expectedCount(expectedCount)
                .initialBuffer(4)
                .startImmediately(false)
                .noProgressLogging()
                .logger(null)
                .build()
            ) {
                ByteArrayOutputStream out = new ByteArrayOutputStream();

                int r;
                byte[] buffer = new byte[10];
                while ((r = inputStream.read(buffer)) != -1) {
                    out.write(buffer, 0, r);
                    log.info("Read {}", r);
                }
                log.info("EOF !, {}", r);
                throw new AssertionFailedError("should not reach this, it should have been interrupted!");

            }
        } catch (ClosedByInterruptException | InterruptedIOException ie) {
            isInterrupted = true;
            log.info("Catched {}", ie.getClass() + " " + ie.getMessage());

        } finally {
            isInterrupted |= thisThread.isInterrupted();
            closed.getAndIncrement();
            log.info("Finally: interrupted: {}: times: {} ", thisThread.isInterrupted(), interrupted.get());
        }
        assertThat(isInterrupted).withFailMessage("Thread did not get interrupted").isTrue();
    }

    protected static FileCachingInputStream slowReader(boolean downloadFirst) {
        return FileCachingInputStream.builder()
            .outputBuffer(2)
            .batchSize(3)
            .noProgressLogging()
            .batchConsumer(throttle(Duration.ofMillis(20)))
            .input(new ByteArrayInputStream(MANY_BYTES))
            .initialBuffer(4)
            .startImmediately(true)
            .downloadFirst(downloadFirst)
            .build();
    }

    @Test
    public void testReadNoAutoStart() throws IOException {
        List<String> logs = new ArrayList<>();
        try (FileCachingInputStream inputStream = FileCachingInputStream.builder()
            .outputBuffer(2)
            .batchSize(3)
            .input(new ByteArrayInputStream(MANY_BYTES))
            .initialBuffer(4)
            .batchConsumer((fc) -> {
                if (fc.isReady()) {
                    logs.add("batch consumes " + fc.getCount());
                }
            })
            .noProgressLogging()
            .startImmediately(false)
            .build()) {
            inputStream.getFuture().thenApply(fc -> {
                logs.add("then apply " + fc.getCount());
                return fc;
            });
            inputStream.getFuture().thenAccept(fc ->
                logs.add("then apply again " + fc.getCount())
            );

            ByteArrayOutputStream out = new ByteArrayOutputStream();

            int r;
            while ((r = inputStream.read()) != -1) {
                out.write(r);
            }
            assertThat(out.toByteArray()).containsExactly(MANY_BYTES);
        }

        assertThat(logs).containsExactly(
            "batch consumes " + MANY_BYTES.length,
            "then apply again " + MANY_BYTES.length,
            "then apply " + MANY_BYTES.length
        );
    }


    @Test
    public void testReadLargeBuffer() throws IOException {

        assertThat(FileCachingInputStream.DEFAULT_INITIAL_BUFFER_SIZE).isGreaterThan(MANY_BYTES.length);
        ByteArrayOutputStream out = new ByteArrayOutputStream();

        try(FileCachingInputStream inputStream = FileCachingInputStream.builder()
            .outputBuffer(2)
            .batchSize(3)
            .input(new ByteArrayInputStream(MANY_BYTES))
            .initialBuffer(null)
            .noProgressLogging()
            .build()) {

            assertThat(inputStream.getBufferLength()).isEqualTo(MANY_BYTES.length);


            int r;
            while ((r = inputStream.read()) != -1) {
                out.write(r);
            }

        }
        out.close();
        assertThat(out.toByteArray()).hasSize(MANY_BYTES.length);

        assertThat(out.toByteArray()).containsExactly(MANY_BYTES);
    }



    @RepeatedTest(5)
    public void testSimple() throws IOException {
        FileCachingInputStream inputStream = FileCachingInputStream.builder()
            .outputBuffer(2)
            .batchSize(3)
            .input(new ByteArrayInputStream(MANY_BYTES))
            .initialBuffer(4)
            .noProgressLogging()
            .startImmediately(false)
            .build();
        ByteArrayOutputStream out = new ByteArrayOutputStream();

        IOUtils.copyLarge(inputStream, out);

        inputStream.close();

        assertThat(inputStream.getCopier().getFuture()).isDone();

        assertThat(out.toByteArray()).containsExactly(MANY_BYTES);
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    public void testTempFile(boolean deleteTempFile) throws IOException {
        FileCachingInputStream inputStream = FileCachingInputStream.builder()
            .outputBuffer(2)
            .batchSize(3)
            .input(new ByteArrayInputStream(MANY_BYTES))
            .initialBuffer(4)
            .noProgressLogging()
            .deleteTempFile(deleteTempFile)
            .startImmediately(false)
            .build();
        ByteArrayOutputStream out = new ByteArrayOutputStream();

        IOUtils.copyLarge(inputStream, out);

        inputStream.close();

        assertThat(inputStream.getTempFile()).isNotNull();
        if (deleteTempFile) {
            assertThat(inputStream.getTempFile()).doesNotExist();
        } else {
            assertThat(inputStream.getTempFile()).exists();
            assertThat(inputStream.getTempFile()).hasBinaryContent(MANY_BYTES);
            Files.delete(inputStream.getTempFile());
        }


        assertThat(out.toByteArray()).containsExactly(MANY_BYTES);
    }

    @Test
    public void testNoAutostart() throws IOException {
        try (
            FileCachingInputStream inputStream = FileCachingInputStream.builder()
                .outputBuffer(2)
                .batchSize(3)
                .input(new ByteArrayInputStream(MANY_BYTES))
                .initialBuffer(0)
                .startImmediately(false)
                .noProgressLogging()
                .build()) {

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
                .initialBuffer(HELLO.length)
                .tempDir("file://tmp/test")
                .build();
            ByteArrayOutputStream out = new ByteArrayOutputStream()
        ) {

            assertThat(inputStream.getBufferLength()).isEqualTo(HELLO.length);

            IOUtils.copy(inputStream, out);
            assertThat(out.toByteArray()).containsExactly(HELLO);
        }
    }

    @Test
    public void testWithLargeBufferByte() throws IOException {
        try (
            FileCachingInputStream inputStream = FileCachingInputStream.builder()
                .outputBuffer(2)
                .batchSize(3)
                .input(new ByteArrayInputStream(HELLO))
                .initialBuffer(2048)
                .build()) {


            ByteArrayOutputStream out = new ByteArrayOutputStream();
            int n;
            while (EOF != (n = inputStream.read())) {
                out.write(n);
            }
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
                .build()) {


            ByteArrayOutputStream out = new ByteArrayOutputStream();

            IOUtils.copy(inputStream, out);

            assertThat(out.toByteArray()).containsExactly(HELLO);
        }
    }


    @Test
    public void testWaitForBytes() throws IOException, InterruptedException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try (
            FileCachingInputStream inputStream = FileCachingInputStream.builder()
                .outputBuffer(2)
                .batchSize(1)
                .input(new ByteArrayInputStream(HELLO))
                .initialBuffer(4)
                .build()) {

            long count = inputStream.waitForBytesRead(10);
            log.info("Found {}", count);
            assertThat(count).isGreaterThanOrEqualTo(1);

            IOUtils.copy(inputStream, out);

            assertThat(inputStream.getCount()).isEqualTo(HELLO.length);


        }
        assertThat(out.toByteArray()).containsExactly(HELLO);
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
    public void ioExceptionFromSourceReadBytes() throws IOException {
        try (
            // an input stream that  will throw IOException when it's busy with file buffering
            InputStream in = new InputStream() {
                private int byteCount = 0;

                @Override
                public int read() throws IOException {
                    if (byteCount == (SIZE_OF_BIG_STREAM / 2)) {
                        throw new IOException("breaking!");
                    }
                    return byteCount++ < SIZE_OF_BIG_STREAM ? 'a' : -1;
                }
            };
            FileCachingInputStream stream = FileCachingInputStream.builder()
                .outputBuffer(2)
                .noProgressLogging()
                .batchSize(100)
                .input(in)
                .logger(log)
                .initialBuffer(4)
                .build()) {

            byte[] buffer = new byte[500];


            assertThatThrownBy(() -> {
                int read = 0;
                int n;
                while (EOF != (n = stream.read(buffer))) {
                    assertThat(n).isNotEqualTo(0); // cannot happen (unless buffer length == 0) according to contract
                    read += n;
                    log.debug("Read {}/{}", n, read);
                }
                }
            )
                .isInstanceOf(IOException.class)
                .hasMessage("breaking!");

            assertThat(stream.getFuture()).isCompletedExceptionally();
            assertThat(stream.available()).isEqualTo(0);
            assertThat(stream.getCopier().isReady()).isTrue();
            assertThatThrownBy(() ->
                stream.getCopier().isReadyIOException()
            ) .isInstanceOf(IOException.class)
                .hasMessage("breaking!");

        }
    }

    @Test
    public void ioExceptionFromSourceReadByte() throws IOException {
        try (
            // an input stream that  will throw IOException when it's busy with file buffering
            InputStream in = new InputStream() {
                private int byteCount = 0;

                @Override
                public int read() throws IOException {
                    if (byteCount == (SIZE_OF_BIG_STREAM / 2)) {
                        throw new IOException("breaking!");
                    }
                    return byteCount++ < SIZE_OF_BIG_STREAM ? 'a' : -1;
                }
            };
            FileCachingInputStream stream = FileCachingInputStream.builder()
                .outputBuffer(200)
                .noProgressLogging()
                .batchSize(100)
                .input(in)
                .logger(log)
                .initialBuffer(4)
                .build()) {

            assertThatThrownBy(() -> {
                int read = 0;
                while (EOF != (stream.read())) {
                    read++;
                    log.trace("Read {}", read);
                }
            }).isInstanceOf(IOException.class)
                .hasMessage("breaking!");
        }
    }


    @SuppressWarnings("ResultOfMethodCallIgnored")
    @Test
    public void createPath() throws IOException {
        new File("/tmp/bestaatniet").delete();
        try (FileCachingInputStream ignored = FileCachingInputStream.builder()
            .outputBuffer(2)
            .batchSize(1)
            .tempDir("/tmp/bestaatniet")
            .input(new ByteArrayInputStream(HELLO))
            .initialBuffer(HELLO.length)
            .build()) {

            assertThat(new File("/tmp/bestaatniet")).exists();
        }
    }


    /**
     * Use {@link FileCachingInputStream} to wrap a huge stream of random bytes (random, so might catch edge cases we didn't think of)
     */
    @SuppressWarnings("UnusedAssignment")
    @Test
    public void performanceBenchmarkAndVerify() throws IOException {
        log.info("Using seed {}", SEED_FOR_LARGE_RANDOM_FILE);
        final int bufferSize = 8192;


        // copy a huge stream using file caching input stream
        File fileCachingDestination = new File("/tmp/fileCaching.bytes");
        fileCachingDestination.deleteOnExit();
        try (
            FileCachingInputStream inputStream = FileCachingInputStream.builder()
                .input(randomStream(SIZE_OF_HUGE_STREAM))
                .initialBuffer(0)  // Setting it to > 0, will fail the compare because Random#nextBytes() will sometimes skip values values
                .outputBuffer(bufferSize)
                .startImmediately(true)
                .noProgressLogging()
                .build();
            OutputStream out = new BufferedOutputStream(new FileOutputStream(fileCachingDestination), bufferSize)
        ) {
            Instant now = Instant.now();
            IOUtils.copyLarge(inputStream, out);
            assertThat(inputStream.getCount()).isEqualTo(SIZE_OF_HUGE_STREAM);
            log.info("Duration when using file caching input stream: {}, {} bytes", Duration.between(now, Instant.now()), SIZE_OF_HUGE_STREAM);
        }

        // check that it arrived correctly

        // size
        assertThat(fileCachingDestination).hasSize(SIZE_OF_HUGE_STREAM);

        // and also check contents of produced file
        Random random = new Random(SEED_FOR_LARGE_RANDOM_FILE);
        int count = 0;
        try (InputStream in = new FileInputStream(fileCachingDestination)) {
            byte[] buf = new byte[8000];
            int read = in.read(buf);
            byte[] compare = new byte[read];
            nextBytes(random, count, compare, 0, read);
            count += read;
            assertThat(buf).startsWith(compare);
        }

        // compare with a normal implementation using used buffered streams.
        File normalDestination = new File("/tmp/normal.bytes");
        normalDestination.deleteOnExit();
        try (

            InputStream inputStream = new BufferedInputStream(randomStream(SIZE_OF_HUGE_STREAM), bufferSize);
            OutputStream out = new BufferedOutputStream(new FileOutputStream(normalDestination), bufferSize)
        ) {
            Instant now = Instant.now();
            IOUtils.copyLarge(inputStream, out);
            log.info("Duration when just using memory buffered streams: {}, {} bytes", Duration.between(now, Instant.now()), SIZE_OF_HUGE_STREAM);
        }
        assertThat(normalDestination).hasSize(SIZE_OF_HUGE_STREAM);

        //assertThat(normalDestination).hasSameBinaryContentAs(fileCachingDestination); // Will do an in memory comparison, this is unusable.


    }

    /**
     * Produces a stream of {@code size} random bytes
     */

    @SuppressWarnings("SameParameterValue")
    private InputStream randomStream(final int size) {
        final Random random = new Random(SEED_FOR_LARGE_RANDOM_FILE);

        return new InputStream() {
            int count = 0;
            @Override
            public int read() {
                if (count++ > size) {
                    return EOF;
                } else {
                    return (byte) random.nextInt();
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
                return size + " random bytes (seed: " + SEED_FOR_LARGE_RANDOM_FILE + ")";
            }

        };
    }

    /**
     * This can be used in stead of {@link #nextBytes(Random, int, byte[], int, int)} to make an entirely predictable stream of bytes, which can be usefull during debugging.
     */
    protected void nextBytes(int count,  byte[] b, int off, int l) {
        for (int i = off; i < off + l; i++)  {
            b[i] = (byte) (count + i);
        }

    }

    protected void nextBytes(Random random, int count,  byte[] b, int off, int l) {
        if (off == 0 && l == b.length) {
            random.nextBytes(b);
        } else {
            byte[] bytes = new byte[l];
            random.nextBytes(bytes);
            System.arraycopy(bytes, 0, b, off, l);
        }
     /*   for (int i = off; i < off + l; i++)  {
            b[i] = (byte) random.nextInt();
        }*/

    }
}
