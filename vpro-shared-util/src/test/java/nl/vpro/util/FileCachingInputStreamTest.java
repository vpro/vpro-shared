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
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.parallel.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

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
    private static final byte[] HELLO = new byte[]{'h', 'e', 'l', 'l', 'o', ' ', 'w', 'o', 'r', 'l', 'd', '!'};
    private static final byte[] MANY_BYTES;
    static {
        int ncopies = 100;
        MANY_BYTES = new byte[HELLO.length * ncopies];
        for (int i = 0; i < ncopies; i++) {
            System.arraycopy(HELLO, 0, MANY_BYTES, i * HELLO.length, HELLO.length);
        }
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
            if (FileCachingInputStream.openStreams.get() != 0) {
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
            while ((r = inputStream.read(buffer)) != -1) {
                out.write(buffer, 0, r);
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


    public static List<InputStream> slowAndNormal() {
        return  Arrays.asList(
            new ByteArrayInputStream(MANY_BYTES),
            new InputStream() {
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
            }
        );
    }


    @ParameterizedTest
    @MethodSource("slowAndNormal")
    public void testReadFileGetsInterrupted(InputStream input) throws IOException {
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
                .initialBuffer(4)
                .startImmediately(false)
                .noProgressLogging()
                .build()
            ) {
                ByteArrayOutputStream out = new ByteArrayOutputStream();

                int r;
                byte[] buffer = new byte[10];
                while ((r = inputStream.read(buffer)) != -1) {
                    out.write(buffer, 0, r);
                }
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

    protected static FileCachingInputStream slowReader(boolean downloadFirst) {
        return FileCachingInputStream.builder()
            .outputBuffer(2)
            .batchSize(3)
            .noProgressLogging()
            .batchConsumer(FileCachingInputStream.throttle(Duration.ofMillis(5)))
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
                    logs.add("" + fc.getCount());
                }
            })
            .noProgressLogging()
            .startImmediately(false)
            .build()) {
            inputStream.getFuture().thenApply(fc -> {
                logs.add("then apply " + fc.getBytesRead());
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
            assertThat(logs).containsExactly("" + MANY_BYTES.length, "then apply again " + MANY_BYTES.length, "then apply 0");
        }
    }


    @Test
    public void testReadLargeBuffer() throws IOException {

        try(FileCachingInputStream inputStream = FileCachingInputStream.builder()
            .outputBuffer(2)
            .batchSize(3)
            .input(new ByteArrayInputStream(MANY_BYTES))
            .initialBuffer(1024)
            .noProgressLogging()
            .build()) {

            ByteArrayOutputStream out = new ByteArrayOutputStream();

            int r;
            while ((r = inputStream.read()) != -1) {
                out.write(r);
            }

            assertThat(out.toByteArray()).containsExactly(MANY_BYTES);
        }
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
                .initialBuffer(4)
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
                //.initialBuffer(2048)
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
        int sizeOfStream = 10000;
        try (
            // an input stream that  will throw IOException when it's busy with file buffering
            InputStream in = new InputStream() {
                private int byteCount = 0;

                @Override
                public int read() throws IOException {
                    if (byteCount == (sizeOfStream / 2)) {
                        throw new IOException("breaking!");
                    }
                    return byteCount++ < sizeOfStream ? 'a' : -1;
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
        int sizeOfStream = 10000;
        try (
            // an input stream that  will throw IOException when it's busy with file buffering
            InputStream in = new InputStream() {
                private int byteCount = 0;

                @Override
                public int read() throws IOException {
                    if (byteCount == (sizeOfStream / 2)) {
                        throw new IOException("breaking!");
                    }
                    return byteCount++ < sizeOfStream ? 'a' : -1;
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
        try (FileCachingInputStream inputStream = FileCachingInputStream.builder()
            .outputBuffer(2)
            .batchSize(1)
            .tempDir("/tmp/bestaatniet")
            .input(new ByteArrayInputStream(HELLO))
            .initialBuffer(HELLO.length)
            .build()) {

            assertThat(new File("/tmp/bestaatniet")).exists();
        }
    }

    @Test
    @Disabled
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
    @Disabled
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
    @Disabled
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
    @Disabled
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
