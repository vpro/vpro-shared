package nl.vpro.util;

import lombok.*;
import lombok.extern.slf4j.Slf4j;

import java.io.*;
import java.net.URI;
import java.nio.file.*;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;

import org.apache.commons.io.IOUtils;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.event.Level;

import com.google.common.annotations.VisibleForTesting;

import nl.vpro.logging.Slf4jHelper;

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

    static final int DEFAULT_INITIAL_BUFFER_SIZE = 2048;
    static final int DEFAULT_FILE_BUFFER_SIZE = 8192;

    static final int EOF = -1;
    @Getter(AccessLevel.PACKAGE)
    @VisibleForTesting
    private final Copier copier;
    private final byte[] buffer;
    @Getter(AccessLevel.PACKAGE)
    @VisibleForTesting
    private final int bufferLength;

    private final Path tempFile;
    private final boolean deleteTempFile;

    private final InputStream tempFileInputStream;
    private boolean tempFileInputStreamClosed = false;

    private volatile boolean closed = false;
    private final AtomicLong count = new AtomicLong(0);
    static final AtomicInteger openStreams = new AtomicInteger(0);

    private Logger log = LoggerFactory.getLogger(FileCachingInputStream.class);

    @Getter
    private final CompletableFuture<FileCachingInputStream> future = new CompletableFuture<>();


    @Slf4j
    public static class Builder {

        /**
         * Calls {@link #path} but with an uri argument
         */
        public Builder tempDir(URI uri) {
            return path(Paths.get(uri));
        }

        /**
         * Calls {@link #path} but with an string argument
         */
        public Builder tempDir(String uri) {
            try {
                return tempDir(URI.create(uri));
            } catch (IllegalArgumentException iae) {
                log.debug("{}:{} Supposing it a file name", uri, iae.getMessage());
                return path(Paths.get(uri));
            }
        }

        public Builder tempFile(Path path) {
            return tempPath(path);
        }


        public Builder tempFile(File file) {
            return tempPath(file.toPath());
        }

        public Builder noProgressLogging() {
            return progressLogging(false);
        }

    }

    /**
     * @param batchSize Batch size
     * @param batchConsumer After reading every batch, you have the possibility to do something yourself too
     * @param path Directory for temporary files
     * @param tempPath Path to temporary file to use
     * @param logger The logger to which possible logging will happen. Defaults to the logger of the {@link FileCachingInputStream} class itself
     * @param progressLogging Wether progress logging must be done (every batch)
     * @param progressLoggingBatch every this many batches a progress logging will be issued (unused progressLogging is explictely false)
     * @param deleteTempFile Whether the intermediate temporary file must be deleted immediately on closing of this stream
     */
    @lombok.Builder(builderClassName = "Builder")
    @SneakyThrows(IOException.class)
    private FileCachingInputStream(
        final InputStream input,
        final Long expectedCount,
        final Path path,
        final String filePrefix,
        final long batchSize,
        final Consumer<FileCachingInputStream> batchConsumer,
        Integer outputBuffer,
        final Logger logger,
        List<OpenOption> openOptions,
        Integer initialBuffer,
        final Boolean startImmediately,
        final Boolean downloadFirst,
        final Boolean progressLogging,
        final Integer progressLoggingBatch,
        final Path tempPath,
        final Boolean deleteTempFile
    ) {
        super();
        if (logger != null) {
            this.log = logger;
        }

        this.deleteTempFile = deleteTempFile == null ? tempPath == null : deleteTempFile;
        if (! this.deleteTempFile) {
            Slf4jHelper.log(log, initialBuffer == null ? Level.WARN : Level.DEBUG,
                "Initial buffer size {} > 0, if input smaller than this no temp file will be created. This may be unexpected since you specified not to delete the temp file.", initialBuffer == null ? DEFAULT_INITIAL_BUFFER_SIZE : initialBuffer);
        }
        if (initialBuffer == null) {
            initialBuffer = DEFAULT_INITIAL_BUFFER_SIZE;
        }

        try {
            if (initialBuffer > 0) {
                // first use an initial buffer of memory only
                byte[] buf = new byte[initialBuffer];

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

                bufferLength = bufferOffset;

                if (complete) {
                    // the buffer was not completely filled.
                    // there will be no need for a file at all.
                    buffer = buf;
                    System.arraycopy(buf, 0, buffer, 0, bufferLength);

                    // don't use file later on
                    copier = null;
                    this.tempFile = null;
                    if (tempPath != null) {
                        try (OutputStream out = Files.newOutputStream(tempPath)) {
                            IOUtils.copy(new ByteArrayInputStream(buffer), out);
                        }
                    }
                    tempFileInputStream = null;
                    log.info("the stream completely fit into the memory buffer");
                    return;
                } else {
                    buffer = buf;
                }
            } else {
                bufferLength = 0;
                buffer = null;
            }

            { // if arriving here, a temp file will be needed
                if (path != null) {
                    if (tempPath != null) {
                        throw new IllegalArgumentException("Specify either path or tempPath (or none), but not both");
                    }
                    if (!Files.isDirectory(path)) {
                        Files.createDirectories(path);
                        log.info("Created directory {}", path);
                    }
                }

                this.tempFile = tempPath == null ? Files.createTempFile(
                    path == null ? Paths.get(System.getProperty("java.io.tmpdir")) : path,
                    filePrefix == null ? "file-caching-inputstream" : filePrefix,
                    null) : tempPath;

                log.debug("Using {}", tempFile);
            }
            if (outputBuffer == null) {
                outputBuffer = DEFAULT_FILE_BUFFER_SIZE;
            }


            final OutputStream tempFileOutputStream = new BufferedOutputStream(Files.newOutputStream(tempFile), outputBuffer);
            incStreams(tempFileOutputStream);
            if (buffer != null) {
                // write the initial buffer to the temp file too, so that this file accurately describes the entire stream
                tempFileOutputStream.write(buffer, 0, bufferLength);
                tempFileOutputStream.flush();
            }

            final Consumer<FileCachingInputStream> consumer;
            if ((progressLogging == null || progressLogging || progressLoggingBatch != null) && !(progressLogging != null && ! progressLogging)) {
                final AtomicLong batchCount = new AtomicLong(0);
                consumer = t -> {
                    if (progressLoggingBatch == null || batchCount.incrementAndGet() % progressLoggingBatch == 0) {
                        log.info("Creating {} ({} bytes written)", tempFile, t.copier.getCount());
                    }
                    if (batchConsumer != null) {
                        batchConsumer.accept(t);
                    }
                };
            } else {
                consumer = batchConsumer == null ?  (t) -> { } : batchConsumer;
            }

            final boolean deleteOnClose = this.deleteTempFile;
            if (openOptions == null) {
                openOptions = new ArrayList<>();
            }
            final boolean effectiveProgressLogging;
            if (progressLogging == null) {
                effectiveProgressLogging = ! deleteOnClose;
            } else {
                effectiveProgressLogging = progressLogging;
            }
            this.tempFileInputStream = new BufferedInputStream(Files.newInputStream(tempFile, openOptions.toArray(new OpenOption[0])));
            incStreams(tempFileInputStream);
             // The copier is responsible for copying the remaining of the stream to the file
            // in a separate thread
            copier = Copier.builder()
                .input(input)
                .expectedCount(expectedCount)
                .offset(bufferLength)
                .output(tempFileOutputStream)
                .name(tempFile.toString())
                .notify(this)
                .errorHandler((c, e) ->
                    future.completeExceptionally(e)
                )
                .callback(c -> {
                    log.debug("callback for copier {} {}", c.getCount(), tempFileOutputStream);
                    try {
                        closeAndDecStreams("file output", tempFileOutputStream); // output is now closed
                        log.info("{} {} {}", c.isReady(), tempFile, tempFile.toFile().length());
                        consumer.accept(FileCachingInputStream.this);
                        log.debug("accepted {}", consumer);
                        future.complete(this);

                    } catch (IOException ioe) {
                        future.completeExceptionally(ioe);
                    }
                    Slf4jHelper.debugOrInfo(log, effectiveProgressLogging, "Created {} ({} bytes written)", tempFile, c.getCount());
                })
                .batch(batchSize)
                .batchConsumer(c -> consumer.accept(this))
                .build();

            if (downloadFirst != null && downloadFirst) {
                copier.execute();
                future.get();
            } else if (startImmediately == null || startImmediately) {
                // if not started immediately, the copier will only be started if the first byte it would produce is actually needed.
                copier.execute();
            }

        } catch (IOException e) {
            log.error(e.getMessage(), e);
            throw e;
        } catch (InterruptedException e) {
            log.error(e.getMessage(), e);
            Thread.currentThread().interrupt();
            throw  new RuntimeException(e);
        } catch (ExecutionException e) {
            log.error(e.getMessage(), e);
            throw new RuntimeException(e);
        }
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
            log.debug("From buffer");

            int result =  readFromBuffer(b, off, len);
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
                if (Files.deleteIfExists(tempFile)) {
                    log.debug("Deleted {}", tempFile);
                } else {
                    //   openOptions.add(StandardOpenOption.DELETE_ON_CLOSE); would have arranged that!
                    log.debug("Could not delete because didn't exists any more {}", tempFile);
                }
            }
            tempFileInputStreamClosed = true;
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

            if (copier != null) {
                // if somewhy closed when copier is not ready yet, it can be interrupted, because we will not be using it any more.
                log.debug("Closing copier");
                try {
                    copier.waitForAndClose();
                } catch (InterruptedException interruptedException) {
                    throw new InterruptedIOException(interruptedException.getMessage());
                }
            } else {
                log.info("No copier to close");
            }
            if (this.tempFile != null && this.deleteTempFile) {
                try {
                    log.debug("Deleting {}", tempFile);
                    Files.deleteIfExists(tempFile);
                } catch (IOException ioException) {
                    log.debug(ioException.getMessage());
                }
            }
        } else {
            log.debug("Closed already", new Exception());
        }

         log.debug("closed");
    }
    public boolean isClosed() {
        return closed;
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
        if (copier != null) {
            copier.executeIfNotRunning();
            while (copier.getCount() < atLeast && ! copier.isReady()) {
                wait();
            }
            return copier.getCount();
        } else {
            return bufferLength;
        }
    }

    /**
     * Returns the number of bytes consumed from the input stream so far
     */
    public long getCount() {
        return copier == null ? bufferLength : copier.getCount();
    }

    /**
     * Returns whether consuming the inputstream is ready.
     */
    public boolean isReady() {
        return copier == null || copier.isReady();
    }

    /**
     * Returns the exception that may have happened. E.g. for use in the call back.
     */
    public Optional<Throwable> getException() {
        return copier == null ? Optional.empty(): copier.getException();
    }

    /**
     * If a temp file is used for buffering, you can may obtain it.
     */
    public Path getTempFile() {
        return tempFile;
    }

    /**
     * One of the paths of {@link #read()}, when it is reading from memory.
     */
    private int readFromBuffer() {
        if (count.get() < bufferLength) {
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
        int toCopy = Math.min(len, bufferLength - (int) count.get() /* remaining bytes in buffer */);
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
        copier.executeIfNotRunning();
        int result = tempFileInputStream.read();
        while (result == EOF) {
            log.debug("EOF, waiting");
            synchronized (copier) {
                while (!copier.isReadyIOException() && result == EOF) {
                    log.debug("Copier {} not yet ready", copier);
                    // copier is still busy, wait a second, and try again.
                    try {
                        copier.wait(1000);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        log.error(e.getMessage(), e);
                        this.close();
                        break;
                    }
                    result = tempFileInputStream.read();
                    log.debug("Read {}", result);
                }
                if (copier.isReadyIOException() && result == EOF) {
                    // the copier did not return any new results
                    // don't increase count but return now.
                    log.debug("Copier is ready ({} bytes), no new results", copier.getCount());
                    return EOF;
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
        copier.executeIfNotRunning();
        if (copier.isReadyIOException() && count.get() == copier.getCount()) {
            log.debug("Count reached {}", count);
            return EOF;
        }
        int result;
        synchronized (copier) {
            result = tempFileInputStream.read(b, offset, length);

            while (!copier.isReadyIOException() && result == EOF) {
                log.debug("Copier {} {}  {} not yet ready", copier.getCount(), count.get(), result);
                try {
                    copier.wait(1000);
                } catch (InterruptedException e) {
                    log.warn("Interrupted {}", e.getMessage());
                    copier.close();
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
                log.debug("EOF {} {}", count.get(), copier.getCount());
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
}
