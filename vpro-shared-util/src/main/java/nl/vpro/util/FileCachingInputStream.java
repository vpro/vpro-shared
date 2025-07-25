package nl.vpro.util;

import lombok.*;
import lombok.extern.slf4j.Slf4j;

import java.io.*;
import java.net.URI;
import java.nio.file.*;
import java.time.Duration;
import java.util.Arrays;
import java.util.Optional;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;

import org.apache.commons.io.IOUtils;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.annotations.VisibleForTesting;

import nl.vpro.logging.simple.SimpleLogger;
import nl.vpro.logging.simple.Slf4jSimpleLogger;

/**
 * <p>When wrapping this around your inputstream, it will be read as fast a possible, but you can
 * consume from it slower. </p>
 *
 * <p>It will first buffer to an internal byte array (if the initial buffer size > 0, defaults to 2048). If that is too small it will buffer the result to a temporary file.
 * </p>
 * <p>Use this if you want to consume an inputstream as fast as possible, while handing it at a
 * slower pace. The cost is the creation of the temporary file.</p>
 *
 * @author Michiel Meeuwissen
 * @since 0.50
 */

public class FileCachingInputStream extends InputStream {

    static final int DEFAULT_INITIAL_BUFFER_SIZE = 8192;
    static final int DEFAULT_FILE_BUFFER_SIZE = 8192;
    static final int EOF = -1;
    static final AtomicInteger openStreams = new AtomicInteger(0);

    @Getter(AccessLevel.PACKAGE)
    @VisibleForTesting
    private final Copier toFileCopier;

    private final byte[] buffer;

    /**
     *  If a temp file is used for buffering, you may obtain it.
     */
    @Getter
    private final Path tempFile;
    private final boolean deleteTempFile;

    private final InputStream tempFileInputStream;
    private boolean tempFileInputStreamClosed = false;

    @Getter
    private volatile boolean closed = false;
    private final AtomicLong count = new AtomicLong(0);
    private final SimpleLogger log ;

    @Getter
    private final Long expectedCount;

    @Getter
    private final CompletableFuture<FileCachingInputStream> future = new CompletableFuture<>();

    /**
     * @param batchSize Batch size/buffer size used when copying
     * @param batchConsumer After reading every batch, you have the possibility to do something yourself too
     * @param path Directory for temporary files
     * @param tempPath Path to temporary file to use
     * @param logger The logger to which possible logging will happen. Defaults to the logger of the {@link FileCachingInputStream} class itself
     * @param downloadFirst If true, then the entire inputstream will be consumed first (defaults to false)
     * @param progressLogging Whether progress logging must be done (every batch)
     * @param progressLoggingBatch every this many batches a progress logging will be issued (unused progressLogging is explicitly false)
     * @param deleteTempFile Whether the intermediate temporary file must be deleted immediately on closing of this stream
     * @param initialBuffer The initial buffer size to use, defaults to 8192 bytes. If this is larger than the input stream, no temporary file will be created.
     */
    @lombok.Builder(builderClassName = "Builder")
    @SneakyThrows(IOException.class)
    private FileCachingInputStream(
        @NonNull final InputStream input,
        @Nullable final Long expectedCount,
        @Nullable final Path path,
        @Nullable final String filePrefix,
        final long batchSize,
        @Nullable final Consumer<FileCachingInputStream> batchConsumer,
        @Nullable Integer outputBuffer,
        @Nullable final Logger logger,
        @Nullable final SimpleLogger simpleLogger,
        @Nullable Integer initialBuffer,
        @Nullable final Boolean startImmediately,
        @Nullable final Boolean downloadFirst,
        @Nullable final Boolean progressLogging,
        @Nullable final Integer progressLoggingBatch,
        @Nullable final Path tempPath,
        @Nullable final Boolean deleteTempFile,
        @Nullable final ExecutorService executorService
    ) {
        super();
        this.log = simpleLogger == null ?
            Slf4jSimpleLogger.of(logger == null ? LoggerFactory.getLogger(FileCachingInputStream.class) : logger) :
            simpleLogger.chain(Slf4jSimpleLogger.of(logger));
        this.deleteTempFile = deleteTempFile == null ? tempPath == null : deleteTempFile;
        if (initialBuffer == null) {
            initialBuffer = DEFAULT_INITIAL_BUFFER_SIZE;
        }
        this.expectedCount = expectedCount;
        try {
            if (initialBuffer > 0) {
                if (! this.deleteTempFile) {
                    log.debug("Initial buffer size {} > 0, if input smaller than this no temp file will be created. This may be unexpected since you specified not to delete the temp file.",  initialBuffer);
                }
                //  fill an initial buffer in memory only
                InitialBufferResult initialBufferResult =
                    fillInitialBuffer(initialBuffer, input, tempPath);
                this.buffer = initialBufferResult.buffer;
                if (initialBufferResult.complete) {
                    // the buffer was sufficiently large to contain the entire stream
                    // there will be no need to read from a file input stream at all
                    this.toFileCopier = null;
                    this.tempFileInputStream = null;
                    this.tempFile = initialBufferResult.tempFile;
                    return;
                }
            } else {
                this.buffer = new byte[0];
            }

            // if arriving here, a temp file will be needed
            this.tempFile = createTempFile(path, tempPath, filePrefix);

            final OutputStream tempFileOutputStream = createTempFileOutputStream(outputBuffer);

            final Consumer<FileCachingInputStream> consumer = assembleEffectiveConsumer(
                progressLogging,
                batchConsumer,
                progressLoggingBatch
            );

            this.tempFileInputStream = new BufferedInputStream(Files.newInputStream(tempFile));
            incStreams(tempFileInputStream);

            toFileCopier = createToFileCopier(
                input,
                this.buffer.length,
                tempFileOutputStream,
                expectedCount,
                consumer,
                batchSize,
                progressLogging,
                executorService
            );
            executeCopier(downloadFirst, startImmediately);
        } catch (IOException e) {
            log.error(e.getMessage(), e);
            throw e;
        } catch (InterruptedException e) {
            log.error(e.getMessage(), e);
            Thread.currentThread().interrupt();
            throw new RuntimeException(e);
        } catch (ExecutionException e) {
            log.error(e.getMessage(), e);
            throw new RuntimeException(e);
        }
    }


    @Override
    public int available() throws IOException {
        if (isClosed() || getException().isPresent()) {
            return 0;
        }
        if (tempFileInputStream == null) {
            return buffer.length - (int) count.get();
        } else {
            toFileCopier.executeIfNotRunning();
            return tempFileInputStream.available();
        }
    }

    /**
     *   Copier is responsible for copying the remaining of the stream to the file
     *   in a separate thread
     */
    private Copier createToFileCopier(
        @NonNull final InputStream input,
        final int offset,
        final OutputStream tempFileOutputStream,
        final Long expectedCount,
        @Nullable final Consumer<FileCachingInputStream> consumer,
        final long batchSize,
        final Boolean progressLogging,
        final ExecutorService executorService
        ) throws ExecutionException, InterruptedException {

        final boolean effectiveProgressLogging;
        if (progressLogging == null) {
            effectiveProgressLogging = ! this.deleteTempFile;
        } else {
            effectiveProgressLogging = progressLogging;
        }
        return Copier.builder()
            .input(input)
            .expectedCount(expectedCount)
            .offset(offset)
            .output(tempFileOutputStream)
            .name(this.tempFile.toString())
            .notify(this)
            .errorHandler((c, e) ->
                this.future.completeExceptionally(e)
            )
            .executorService(executorService)
            .callback(c -> {
                log.debug("callback for copier {} {}", c.getCount(), tempFileOutputStream);
                try {
                    closeAndDecStreams("file output", tempFileOutputStream); // output is now closed
                    log.debug("{} {} {}", c.isReady(), this.tempFile, this.tempFile.toFile().length());
                    if (consumer != null) {
                        consumer.accept(FileCachingInputStream.this);
                        log.debug("accepted {}", consumer);
                    }
                    this.future.complete(this);
                } catch (IOException ioe) {
                    this.future.completeExceptionally(ioe);
                }
                log.debugOrInfo(effectiveProgressLogging, "Created {} ({} ({}) bytes written)", this.tempFile, c.getCount(), FileSizeFormatter.DEFAULT.format(c.getCount()));
            })
            .batch(batchSize)
            .batchConsumer(consumer == null ? null : c -> consumer.accept(this))
            .build();
    }

    private void executeCopier(Boolean downloadFirst, Boolean startImmediately) throws ExecutionException, InterruptedException {
        if (downloadFirst != null && downloadFirst) {
            this.toFileCopier.execute();
            this.future.get();
        } else if (startImmediately == null || startImmediately) {
            // if not started immediately, the copier will only be started if the first byte it would produce is actually needed.
            this.toFileCopier.execute();
        }
    }

    /**
     * Combines the user provided 'batchConsumer' (if there is one), with some other settings, to one 'effective' consumer.
     */
    @Nullable
    private Consumer<FileCachingInputStream> assembleEffectiveConsumer(
        final Boolean progressLogging,
        final Consumer<FileCachingInputStream> batchConsumer,
        final Integer progressLoggingBatch) {
        final Consumer<FileCachingInputStream> consumer;
        if ((progressLogging == null || progressLogging || progressLoggingBatch != null) &&
            !(progressLogging != null && ! progressLogging)) {
            final AtomicLong batchCount = new AtomicLong(0);
            consumer = t -> {
                if (progressLoggingBatch == null ||
                    batchCount.incrementAndGet() % progressLoggingBatch == 0) {
                    log.info("Creating {} ({} bytes ({}) written)", tempFile, t.toFileCopier.getCount(), FileSizeFormatter.DEFAULT.format(t.toFileCopier.getCount()));
                }
                if (batchConsumer != null) {
                    batchConsumer.accept(t);
                }
            };
        } else {
            consumer = batchConsumer;
        }
        return consumer;
    }

    private Path createTempFile(@Nullable Path path, @Nullable Path tempPath, @Nullable String filePrefix) throws IOException {
        // if arriving here, a temp file will be needed
        if (path != null) {
            if (tempPath != null) {
                throw new IllegalArgumentException("Specify either path or tempPath (or none), but not both");
            }
            if (!Files.isDirectory(path)) {
                Files.createDirectories(path);
                log.info("Created directory {}", path);
            }
        }

        final Path tempFile = tempPath == null ? Files.createTempFile(
            path == null ? Paths.get(System.getProperty("java.io.tmpdir")) : path,
            filePrefix == null ? "file-caching-inputstream" : filePrefix,
            null) : tempPath;

        log.debug("Using {}", tempFile);
        return  tempFile;

    }
    private OutputStream createTempFileOutputStream(@Nullable Integer outputBuffer) throws IOException {
        if (outputBuffer == null) {
            outputBuffer = DEFAULT_FILE_BUFFER_SIZE;
        }


        final OutputStream tempFileOutputStream = new BufferedOutputStream(Files.newOutputStream(tempFile), outputBuffer);
        incStreams(tempFileOutputStream);
        if (buffer != null) {
            // write the initial buffer to the temp file too, so that this file accurately describes the entire stream
            tempFileOutputStream.write(buffer, 0, buffer.length);
            tempFileOutputStream.flush();
        }
        return  tempFileOutputStream;
    }

    private InitialBufferResult fillInitialBuffer(int initialBuffer, InputStream input, Path tempPath) throws IOException {
        // first use an initial buffer of memory only
        final byte[] buf = new byte[initialBuffer];

        final InitialBufferResult.Builder builder = InitialBufferResult.builder();
        int bufferOffset = 0;
        int numRead;
        boolean complete;
        do {
            numRead = input.read(buf, bufferOffset, buf.length - bufferOffset);
            complete = numRead == EOF;
            if (! complete) {
                bufferOffset += numRead;
            }
        } while (! complete && bufferOffset < buf.length);

        int bufferLength = bufferOffset;

        if (complete) {
            log.debug("The inputstream gave EOF after {} bytes. Completely fitting into memory buffer", bufferLength);
            builder.buffer(Arrays.copyOf(buf, bufferLength));
            if (tempPath != null) {
                // there is no need for the file., but since an explitely file was
                // configured write it to that file anyway
                try (final OutputStream out = Files.newOutputStream(tempPath)) {
                    IOUtils.copy(new ByteArrayInputStream(builder.buffer), out);
                }
                builder.tempFile(tempPath);
            }
            log.debug("the stream completely fit into the memory buffer");
            builder.complete(true);
        } else {
            builder.buffer(buf);
            builder.complete(false);
        }
        return builder.build();
    }



    public int getBufferLength() {
        return buffer.length;
    }

    @Override
    public int read() throws IOException {
        if (tempFileInputStream == null) {
            // the stream was small, we are reading from the memory buffer
            return readFromBuffer();
        } else {
            return readFromFile();
        }
    }


    @Override
    public int read(byte @NonNull[] b, int off, int len) throws IOException {
        if (tempFileInputStream == null) {
            int result =  readFromBuffer(b, off, len);
            log.debug("From buffer {}", result);
            return result;
        } else {
            int result = readFromFile(b, off, len);
            log.trace("From file {}", result);
            return result;
        }
    }


    protected synchronized void closeTempFile() throws IOException {
        if (this.tempFileInputStream != null && ! tempFileInputStreamClosed) {
            closeAndDecStreams("file input", this.tempFileInputStream);
            if (tempFile != null && this.deleteTempFile) {
                deleteTempFile();
            }
            tempFileInputStreamClosed = true;
        }
    }

    public void deleteTempFile() {
        if (tempFile != null) {
            try {
                if (Files.deleteIfExists(tempFile)) {
                    log.debug("Deleted {}", tempFile);
                } else {
                    //   openOptions.add(StandardOpenOption.DELETE_ON_CLOSE); would have arranged that!
                    log.debug("Could not delete because didn't exists any more {}", tempFile);
                }
            } catch(IOException ioe) {
                log.debug("Could not delete {}", tempFile, ioe);
            }
        }
    }

    @Override
    public void close() throws IOException {

        if (! closed) {
            synchronized(this) {
                log.debug("Closing");
                if (closed) {
                    log.debug("Closed by other thread in the mean time");
                    return;
                }
                closeTempFile();

                closed = true;
                notifyAll();
            }

            if (toFileCopier != null) {
                // if somewhy closed when copier is not ready yet, it can be interrupted, because we will not be using it any more.
                log.debug("Closing copier");
                try {
                    toFileCopier.waitForAndClose();
                } catch (InterruptedException interruptedException) {
                    throw new InterruptedIOException(interruptedException.getMessage());
                }
            } else {
                log.debug("No copier to close");
            }
            if (this.tempFile != null && this.deleteTempFile) {
                try {
                    log.debug("Deleting {}", tempFile);
                    Files.deleteIfExists(tempFile);
                } catch (IOException ioException) {
                    log.debug(ioException.getClass().getName() + ": " + ioException.getMessage());
                }
            }
        } else {
            log.debug("Closed already", new Exception());
        }

         log.debug("closed");
    }

    @Override
    public String toString() {
        return super.toString() + " for " + tempFile;
    }

    /**
     * Wait until the copier thread read at least the number of bytes given.
     *
     */
    public synchronized long waitForBytesRead(int atLeast) throws InterruptedException {
        if (toFileCopier != null) {
            toFileCopier.executeIfNotRunning();
            while (toFileCopier.getCount() < atLeast && ! toFileCopier.isReady()) {
                wait();
            }
            return toFileCopier.getCount();
        } else {
            return buffer.length;
        }
    }

    /**
     * Returns the number of bytes consumed from the input stream so far
     */
    public long getCount() {
        return toFileCopier == null ? buffer.length : toFileCopier.getCount();
    }

    /**
     * Returns whether consuming the inputstream is ready.
     */
    public boolean isReady() {
        return toFileCopier == null || toFileCopier.isReady();
    }

    /**
     * Returns the exception that may have happened. E.g. for use in the call back.
     */
    public Optional<Throwable> getException() {
        return toFileCopier == null ? Optional.empty(): toFileCopier.getException();
    }

    /**
     * One of the paths of {@link #read()}, when it is reading from memory.
     */
    private int readFromBuffer() {
        if (count.get() < buffer.length) {
            byte result = buffer[(int) count.getAndIncrement()];
            synchronized (this) {
                notifyAll();
            }
            return Byte.toUnsignedInt(result);
        } else {
            return EOF;
        }
    }

    /**
     * One of the paths of {@link #read(byte[], int, int)} )}, when it is reading from memory.
     */
    private int readFromBuffer(byte[] b, int off, int len) {
        int toCopy = Math.min(len, buffer.length - (int) count.get() /* remaining bytes in buffer */);
        if (toCopy > 0) {
            System.arraycopy(buffer, (int) count.get(), b, off, toCopy);
            synchronized (this) {
                notifyAll();
            }
            count.addAndGet(toCopy);
            return toCopy;
        } else {
            log.debug("EOF from buffer");
            return EOF;
        }
    }

    /**
     *
     * See  {@link InputStream#read()} This methods must behave exactly according to that.
     */
    private int readFromFile() throws IOException {
        toFileCopier.executeIfNotRunning();
        int result = tempFileInputStream.read();
        while (result == EOF) {
            log.debug("EOF, waiting");
            synchronized (toFileCopier) {
                while (!toFileCopier.isReadyIOException() && result == EOF) {
                    log.debug("Copier {} not yet ready", toFileCopier);
                    // copier is still busy, wait a second, and try again.
                    try {
                        toFileCopier.wait(1000);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        log.error(e.getMessage(), e);
                        this.close();
                        break;
                    }
                    result = tempFileInputStream.read();
                    log.debug("Read {}", result);
                }
                if (toFileCopier.isReadyIOException() && result == EOF) {
                    // the copier did not return any new results
                    result = tempFileInputStream.read(); // there may be some bytes written in between last statements
                    if (result == EOF) {
                        // don't increase count but return now.
                        log.debug("Copier is ready ({} bytes), no new results", toFileCopier.getCount());
                        return EOF;
                    }
                }
            }
        }
        //noinspection ConstantConditions
        assert result != EOF;

        count.incrementAndGet();
        log.debug("Returning {}" ,result);
        return result;
    }

    /**
     *
     * See {@link InputStream#read(byte[], int, int)} This methods must behave exactly according to that.
     */
    private int readFromFile(byte[] b, int offset, int length) throws IOException {
        toFileCopier.executeIfNotRunning();
        if (toFileCopier.isReadyIOException() && count.get() == toFileCopier.getCount()) {
            log.debug("Count reached {}", count);
            return EOF;
        }
        int result;
        synchronized (toFileCopier) {
            result = tempFileInputStream.read(b, offset, length);

            while (!toFileCopier.isReadyIOException() && result == EOF) {
                log.debug("Copier {} {}  {} not yet ready", toFileCopier.getCount(), count.get(), result);
                try {
                    toFileCopier.wait(1000);
                } catch (InterruptedException e) {
                    log.warn("Interrupted, message: {}", e.getMessage());
                    toFileCopier.close();
                    future.completeExceptionally(e);
                    close();
                    Thread.currentThread().interrupt();
                    throw new InterruptedIOException(e.getMessage());
                }
                result = tempFileInputStream.read(b, offset, length);
                log.debug("result {}", result);
            }
            if (result == EOF) {
                log.debug("Copier ready, but found EOF");
                result = tempFileInputStream.read(b, offset, length);
            }
            if (result != EOF) {
                count.addAndGet(result);
            } else {
                log.debug("EOF {} {}", count.get(), toFileCopier.getCount());
            }
        }
        assert result != 0;
        //log.debug("returning {} bytes", totalResult);
        return result;
    }

    public static Consumer<FileCachingInputStream> throttle(Duration d) {
        return (fc) -> {
            try {
                Thread.sleep(d.toMillis());
            } catch (InterruptedException ignored) {
                Thread.currentThread().interrupt();

            }
        };
    }

    private void incStreams(Closeable closable) {
        synchronized (openStreams) {
            log.debug("{} opened {}", openStreams.incrementAndGet(), closable);
        }
    }
    private void closeAndDecStreams(String desc, Closeable closable) throws IOException {
        synchronized (openStreams) {
            int i = openStreams.decrementAndGet();
            log.debug("{} closing {} {}", i, desc, closable);
            closable.close();
        }
    }


    @Slf4j
    public static class Builder {

        /**
         * Calls {@link #path} but with an uri argument
         */
        public Builder tempDir(@Nullable URI uri) {
            return path(uri == null ? null : Paths.get(uri));
        }

        /**
         * Calls {@link #path} but with a string argument
         */
        public Builder tempDir(@Nullable String uri) {
            if (uri == null) {
                return tempDir((URI) null);
            }
            try {
                return tempDir(URI.create(uri));
            } catch (IllegalArgumentException iae) {
                log.debug("{}:{} Supposing it a file name", uri, iae.getMessage());
                return path(Paths.get(uri));
            }
        }

        public Builder tempFile(@Nullable Path path) {
            return tempPath(path);
        }

        public Builder tempFile(@Nullable File file) {
            return tempPath(file == null ? null : file.toPath());
        }

        public Builder noProgressLogging() {
            return progressLogging(false);
        }

    }


    @AllArgsConstructor
    @lombok.Builder
    private static class InitialBufferResult {
        final boolean complete;
        final byte[] buffer;
        final Path tempFile;
    }
}
