package nl.vpro.util;

import lombok.extern.slf4j.Slf4j;

import java.io.*;
import java.nio.channels.ClosedByInterruptException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicLong;

import org.apache.commons.io.IOUtils;
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
@TestMethodOrder(MethodOrderer.Random.class)
public class FileCachingInputStreamTest {
    private static final int SEED = new Random().nextInt();
    //private static final int SEED = -118023437; // gave some troubles

    private static final Random RANDOM = new Random(SEED);
    private static final int SIZE_OF_BIG_STREAM = 10_000 + RANDOM.nextInt(5_000);
    private static final int SIZE_OF_HUGE_STREAM = 1_000_000_000 + RANDOM.nextInt(500_000_000);
    private static final int SEED_FOR_LARGE_RANDOM_FILE = RANDOM.nextInt();

    static {
        log.info("SEED {}", SEED);
        log.info("SIZE OF BIG {}", SIZE_OF_BIG_STREAM);
        log.info("SIZE OF HUGE {}", SIZE_OF_HUGE_STREAM);
        log.info("SEED OF LARGE RANDOM {}", SEED_FOR_LARGE_RANDOM_FILE);
    }


    private static final byte[] HELLO = new byte[]{'h', 'e', 'l', 'l', 'o', ' ', 'w', 'o', 'r', 'l', 'd', '!'};
    private static final byte[] MANY_BYTES;
    static {
        MANY_BYTES = new byte[1500];
        RANDOM.nextBytes(MANY_BYTES);
    }



    @BeforeEach
    public void before(TestInfo testInfo) {
        log.info(">-----{}. Interrupted {}, openstreams: {}", testInfo.getDisplayName(), Thread.interrupted(), FileCachingInputStream.openStreams);
        FileCachingInputStream.openStreams.set(0);
    }

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
        log.info("<-----{}. Interrupted {}, openstreams: {}", testInfo.getDisplayName(), wasInterrupted, FileCachingInputStream.openStreams);
        assertThat(FileCachingInputStream.openStreams.get()).isEqualTo(0);
    }

    @ParameterizedTest(name = "{displayName} {arguments}")
    @ValueSource(booleans = {true, false})
    public void slowProduceAndIOUtils(boolean downloadFirst) throws IOException {

        try(FileCachingInputStream inputStream =  slowReader(downloadFirst);
            ByteArrayOutputStream out = new ByteArrayOutputStream()
        ) {
            IOUtils.copy(inputStream, out);
            assertThat(out.toByteArray()).containsExactly(MANY_BYTES);
        }
    }


    /**
     * Tests what happens if the producing inputstream is slow.
     */
    @ParameterizedTest(name = "{displayName}  downloadFirst: {arguments}")
    @ValueSource(booleans = {true, false})
    public void testSlowProduce(boolean downloadFirst) throws IOException {

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


    /**
     * Tests what happens if the producing inputstream is fast, but we consume it slowly.
     */
    @Test
    public void slowConsume() throws IOException, InterruptedException {

        log.info("Size: {}", SIZE_OF_HUGE_STREAM);
        try(FileCachingInputStream inputStream = FileCachingInputStream
            .builder()
            .input(randomStream(SIZE_OF_HUGE_STREAM))
            .progressLoggingBatch(10)
            .downloadFirst(false)
            .batchSize(1_000_000)
            .build()
        ) {

            int result = 0;
            int tresult = 0;
            int r;
            byte[] buffer = new byte[RANDOM.nextInt(900) + 100];
            log.info("Available: {}", inputStream.available());
            while ((r = inputStream.read(buffer, 0, buffer.length)) != -1) {
                result += r;
                tresult += r;
                if (tresult > 10_000_000) {
                    log.info("Read {}", result);
                    tresult = 0;
                }
                if (RANDOM.nextInt(100) == 0) {
                    Thread.sleep(2);
                }
                //out.write(buffer, 1, r);
            }

            assertThat(result).isEqualTo(SIZE_OF_HUGE_STREAM);
        }
    }

    @Test
    public void readFileGetsBroken()  {
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

        int slows = 10;
        int bytearrays = 4;
        int total = bytearrays + slows;
        return new Iterator<>() {
            int count = 0;

            @Override
            public boolean hasNext() {
                return count < total;
            }

            @SuppressWarnings("resource")
            @Override
            public Object[] next() {
                Long expectedSize = count % 2 == 0 ? (long) MANY_BYTES.length : null;
                byte[] bytes = new byte[MANY_BYTES.length];
                {
                    RANDOM.nextBytes(bytes);
                }
                if (count++ < slows) {
                    return new Object[]{new ByteArrayInputStream(bytes), expectedSize};
                } else {
                    return new Object[]{new SlowInputStream(5, bytes), expectedSize};

                }
            }
        };
    }


    @ParameterizedTest(name = "{displayName} {arguments}")
    @MethodSource("slowAndNormal")
    public void readFileGetsInterrupted(InputStream input, Long expectedCount) throws IOException {
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
                    log.debug("count consumer {} ", f.getCount());
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
                            log.debug("{} Interrupted already {} {}", f.getCount(), thisThread, i);
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
                    log.debug("Read {}", r);
                }
                log.info("EOF !, {}", r);
                throw new AssertionFailedError("should not reach this, it should have been interrupted!");
            }
        } catch (ClosedByInterruptException | InterruptedIOException ie) {
            isInterrupted = true;
            log.info("Caught {}", ie.getClass() + " " + ie.getMessage(), ie);
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

    @RepeatedTest(value = 100, name = "{displayName} {currentRepetition}")
    public void readAutoStart(RepetitionInfo repetitionInfo) throws IOException {

        final List<String> logs = new CopyOnWriteArrayList<>();
        final String expected = "[batch consumes " + MANY_BYTES.length + ", then apply " + MANY_BYTES.length + ", then apply again " + MANY_BYTES.length +"]";
        try (FileCachingInputStream inputStream = FileCachingInputStream.builder()
            .outputBuffer(2)
            .batchSize(3)
            .input(new ByteArrayInputStream(MANY_BYTES))
            .initialBuffer(4)
            .batchConsumer((fc) -> {
                if (fc.isReady()) {
                    logs.add("batch consumes " + fc.getCount());
                    if (fc.getCount() < 1500) {
                        log.info("{}", logs, new Exception());
                    }
                }
            })
            .noProgressLogging()
            .startImmediately(repetitionInfo.getCurrentRepetition() % 2 == 0)
            .deleteTempFile(true)
            .path(Paths.get(System.getProperty("java.io.tmpdir"), "filecaching" + repetitionInfo.getCurrentRepetition()))
            .build()) {
            inputStream.getFuture().thenApply(fc -> {
                logs.add("then apply " + fc.getCount());
                return fc;
            }).thenAccept(fc ->
                logs.add("then apply again " + fc.getCount())
            );

            final ByteArrayOutputStream out = new ByteArrayOutputStream();

            int r;
            while ((r = inputStream.read()) != -1) {
                out.write(r);
            }
            byte[] bytes = out.toByteArray();
            assertThat(bytes).hasSize(MANY_BYTES.length);
            assertThat(bytes).containsExactly(MANY_BYTES);
        }
        log.debug("Asserting now");
        assertThat(logs.toString()).isEqualTo(expected);
    }


    @Test
    public void readLargeBuffer() throws IOException {

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
            log.info("Available: {}", inputStream.available());

            int r;
            while ((r = inputStream.read()) != -1) {
                out.write(r);
                log.debug("Available: {}", inputStream.available());

            }
            assertThat(inputStream.available()).isEqualTo(0);
        }
        out.close();
        assertThat(out.toByteArray()).hasSize(MANY_BYTES.length);

        assertThat(out.toByteArray()).containsExactly(MANY_BYTES);
    }



    @RepeatedTest(value = 5, name = "{displayName} {currentRepetition}")
    public void simple() throws IOException {
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

        assertThat(inputStream.getToFileCopier().getFuture()).isDone();

        assertThat(out.toByteArray()).containsExactly(MANY_BYTES);
    }

    @ParameterizedTest(name = "{displayName} {arguments}")
    @ValueSource(booleans = {true, false})
    public void tempFile(boolean deleteTempFile) throws IOException {
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
    public void noAutostart() throws IOException {
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
    public void withLargeBuffer() throws IOException {
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
    public void withLargeBufferByte() throws IOException {
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
    public void withBufferEdge() throws IOException {
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


    @SuppressWarnings("BusyWait")
    @Test
    public void slowInput() throws IOException {
        PipedInputStream pipedInputStream = new PipedInputStream();
        PipedOutputStream outputStream = new PipedOutputStream(pipedInputStream);
        new Thread(() -> {
            InputStream bytes = new ByteArrayInputStream(MANY_BYTES);
            try {
                int r;
                while ((r = bytes.read()) != -1) {
                    outputStream.write(r);
                    Thread.sleep(2);
                }
                log.info("ready");
                outputStream.close();
            } catch (IOException | InterruptedException e) {
                log.error(e.getMessage(), e);
            }

        }).start();
        try (FileCachingInputStream inputStream = FileCachingInputStream.builder()
            .outputBuffer(2)
            .batchSize(3)
            .input(pipedInputStream)
            .initialBuffer(4)
            .noProgressLogging()
            .startImmediately(true)
            .build()) {
            final ByteArrayOutputStream out = new ByteArrayOutputStream();

            IOUtils.copy(inputStream, out);

            byte[] bytes = out.toByteArray();
            assertThat(bytes).hasSize(MANY_BYTES.length);
            assertThat(bytes).containsExactly(MANY_BYTES);
        }

    }


    @Test
    public void waitForBytes() throws IOException, InterruptedException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try (
            FileCachingInputStream inputStream = FileCachingInputStream.builder()
                .outputBuffer(2)
                .batchSize(1)
                .input(new ByteArrayInputStream(HELLO))
                .initialBuffer(4)
                .build()) {

            long count = inputStream.waitForBytesRead(10);
            log.debug("Found {}", count);
            assertThat(count).isGreaterThanOrEqualTo(1);

            IOUtils.copy(inputStream, out);

            assertThat(out.toByteArray()).containsExactly(HELLO);
            assertThat(inputStream.getCount()).isEqualTo(HELLO.length);


        }
        assertThat(out.toByteArray()).containsExactly(HELLO);
    }

    @Test
    public void waitForBytesOnZeroBytes() throws IOException, InterruptedException {
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
                        log.info("Breaking now");
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
            assertThat(stream.getToFileCopier().isReady()).isTrue();
            assertThatThrownBy(() ->
                stream.getToFileCopier().isReadyIOException()
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
        log.info("Using seed {}", SEED);
        final int bufferSize = 8192;


        // copy a huge stream using file caching input stream
        File fileCachingDestination = new File("/tmp/fileCaching.bytes");
        fileCachingDestination.deleteOnExit();
        try (
            FileCachingInputStream inputStream = FileCachingInputStream.builder()
                .input(randomStream(SIZE_OF_HUGE_STREAM))
                .expectedCount((long) SIZE_OF_HUGE_STREAM)
                .initialBuffer(0)  // Setting it to > 0, will fail the compare because Random#nextBytes() will sometimes skip values values
                .outputBuffer(bufferSize)
                .startImmediately(true)
                .deleteTempFile(true)
                .noProgressLogging()
                .build();
            FileOutputStream file = new FileOutputStream(fileCachingDestination);
            OutputStream out = new BufferedOutputStream(file, bufferSize)
        ) {
            Instant now = Instant.now();
            long copied = IOUtils.copyLarge(inputStream, out);
            assertThat(inputStream.getCount()).withFailMessage("COUNT %d != %d", inputStream.getCount(), SIZE_OF_HUGE_STREAM).isEqualTo(SIZE_OF_HUGE_STREAM);
            assertThat(copied).withFailMessage("IO COPY %d != %d", copied, SIZE_OF_HUGE_STREAM).isEqualTo(SIZE_OF_HUGE_STREAM);
            log.info("Duration when using file caching input stream: {}, {} bytes", Duration.between(now, Instant.now()), SIZE_OF_HUGE_STREAM);
        }

        // check that it arrived correctly

        // size
        assertThat(fileCachingDestination).withFailMessage("FILE").hasSize(SIZE_OF_HUGE_STREAM);

        // and also check contents of produced file
        Random random = new Random(SEED_FOR_LARGE_RANDOM_FILE);
        int count = 0;
        try (final InputStream in = Files.newInputStream(fileCachingDestination.toPath())) {
            byte[] buf = new byte[8000];
            int read = in.read(buf);
            byte[] compare = new byte[read];
            nextBytes(random, count, compare, 0, read);
            count += read;
            assertThat(buf).startsWith(compare);
        }
    }

    @Test
    public void performanceBenchmarkAndVerifyIOUtils() throws IOException {
        log.info("Using seed {}", SEED);
        final int bufferSize = 8192;

        // compare with a normal implementation using used buffered streams.
        File normalDestination = new File("/tmp/normal.bytes");
        normalDestination.deleteOnExit();
        try (
            InputStream inputStream = new BufferedInputStream(randomStream(SIZE_OF_HUGE_STREAM), bufferSize);
            OutputStream out = new BufferedOutputStream(Files.newOutputStream(normalDestination.toPath()), bufferSize)
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
        return new RandomStream(SEED_FOR_LARGE_RANDOM_FILE, size);
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
