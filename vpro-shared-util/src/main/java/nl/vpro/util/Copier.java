package nl.vpro.util;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.io.*;
import java.util.Optional;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import org.apache.commons.io.IOUtils;

import static nl.vpro.util.FileCachingInputStream.EOF;

/**
 * Can be used to copy an {@link InputStream} to an {@link OutputStream} in a stand alone thread.
 * Used by {@link FileCachingInputStream}.
 *
 * @author Michiel Meeuwissen
 * @since 3.1
 */
@Slf4j
public class Copier implements Runnable, Closeable {
    static final int MAX_BUFFER = 8192;

    private volatile boolean ready;
    private volatile boolean readyAndCallbacked;

    private volatile Throwable exception;
    private final  AtomicLong count = new AtomicLong(0);

    private final InputStream input;
    private final Long expectedCount;
    private final OutputStream output;
    private final long batch;
    private final Consumer<Copier> callback;
    private final BiConsumer<Copier, Throwable> errorHandler;
    private final Consumer<Copier> batchConsumer;
    @Getter
    private Future<?> future;
    @Getter(AccessLevel.NONE)
    private final String name;
    private final String logPrefix;
    private final Object notify;




    /**
     *
     * @param input The input stream to copy from (will be closed if ready)
     * @param output The output stream to copy to (will not be implicetely closed)
     * @param batch The size of batches (defaults to 8192)
     * @param batchConsumer Some action to perform after each batch
     * @param callback Called when ready, this should probably close the outputstream
     * @param errorHandler Called on error, just before callback
     * @param offset Just the initial value for {@link #getCount()}
     * @param name A name to assign to this copier
     * @param notify if a batch was handled notify this object
     */
    @lombok.Builder(builderClassName = "Builder")
    private Copier(
        InputStream input,
        Long expectedCount,
        OutputStream output,
        long batch,
        Consumer<Copier> batchConsumer,
        Consumer<Copier> callback,
        BiConsumer<Copier, Throwable> errorHandler,
        int offset,
        String name,
        Object notify
        ) {
        this.input = input;
        this.expectedCount = expectedCount;
        this.output = output;
        this.batch = batch == 0 ? MAX_BUFFER: batch;
        this.callback = callback;
        this.batchConsumer = batchConsumer;
        this.errorHandler = errorHandler;
        this.count.set(offset);
        this.name = name;
        this.logPrefix = name == null ? "" : name + ": ";
        this.notify = notify;
    }

    public Copier(InputStream i, OutputStream o, long batch) {
        this(i, null, o, batch, null, null, null,  0, null, null);
    }


    public Copier(InputStream i, OutputStream o) {
        this(i, o, MAX_BUFFER);
    }

    @Override
    public void run() {
        try {
            if (batchConsumer == null || batch < 1) {
                count.addAndGet(IOUtils.copyLarge(input, output));
            } else {
                copyWithBatchCallBacks();
            }
            output.flush();
        } catch (IOException ioe) {
            exception = ioe;
            log.debug(ioe.getMessage());
        } catch (Exception t) {
            if (! CommandExecutor.isBrokenPipe(t)) {
                log.warn("{}Connector {}\n{} {}", logPrefix, toString(), t.getClass().getName(), t.getMessage());
            }
            log.warn(t.getMessage());
            exception = t;
        } finally {
            log.debug("finally");
            afterRun();

        }
    }

    /**
     * We used to use {@link IOUtils#copyLarge(InputStream, OutputStream, long, long)}
     */
    private void copyWithBatchCallBacks() throws IOException {
        final int[] equalParts = equalsParts();
        int part = 0;
        boolean busy = true;
        while (busy) {
            int readInPart = 0;
            byte[] buffer = new byte[equalParts[part]];
            assert buffer.length > 0;
            while (readInPart < buffer.length) {
                int read = input.read(buffer, readInPart, buffer.length - readInPart);
                if (read == EOF) {
                    checkCount(count.get());
                    log.debug("breaking on {}", count.get());
                    busy = false;
                    break;
                }
                readInPart += read;
            }

            output.write(buffer, 0, readInPart);
            count.addAndGet(readInPart);
            if (++part == equalParts.length) { // the required batch consumer size is now full
                batchConsumer.accept(this);
                notifyIfRequested();
                part = 0;
            }
        }
        log.debug("Copied {} from {} to {}", getCount(), input, output);
    }

    private void checkCount(long currentCount) {
        if (expectedCount != null) {
            if (currentCount < expectedCount) {
                log.warn("write insufficient {} < expected {}", count.get(), expectedCount);
            } else {
                log.info("write succeeded {} == {}", count.get(), expectedCount);
            }
        }
    }

    private int[] equalsParts() {
        return equalsParts(batch);
    }

    /**
     * Given a batch size, divide it up in equal parts smaller then 8k in size.
     * @return An array of at least one element. The sum of all elements is the argument. All values are smaller then {@link #MAX_BUFFER}
     */
    static int[] equalsParts(long batch) {
        int parts = (int) (batch / MAX_BUFFER) + 1;
        int[] arr = new int[parts];
        long whole = batch;
        for (int i = 0; i < parts; i++) {
            int v = (int) ((whole + parts - i - 1) / (parts - i));
            arr[i] = v;
            whole -= v;
        }
        return arr;
    }

    private void afterRun() {
        log.debug("Ready");
        synchronized (this) {
            ready = true;
        }
        handleError();
        callBack();
        synchronized(this) {
            readyAndCallbacked = true;
            notifyIfRequested();
        }
    }

    public void waitFor() throws InterruptedException {
        executeIfNotRunning();
        synchronized (this) {
            while (!readyAndCallbacked) {
                wait();
            }
            log.debug("ready");
        }
    }

    public void waitForAndClose() throws InterruptedException, IOException {
        log.debug("waitForAndClose");
        waitFor();
        close();
    }

    /**
     * Checks whether this copier is ready. You may want to check {@link #getException()} or use {@link #isReadyIOException()} to deal with unsuccessfull terminations
     */
    public boolean isReady() {
        return ready;
    }

    /**
     * Checks whether this copier is ready, but will throw an {@link IOException} it it did not _successfully_ finish.
     */
    public boolean isReadyIOException() throws IOException {
        if (readyAndCallbacked) {
            throwIOExceptionIfNeeded();
        }
        return readyAndCallbacked;
    }

    public Optional<Throwable> getException() {
        return Optional.ofNullable(exception);
    }

    private void throwIOExceptionIfNeeded() throws IOException {
        if (exception != null) {
            if (exception instanceof IOException) {
                throw (IOException) exception;
            } else {
                throw new IOException(exception);
            }
        }
    }

    /**
     * Returns the number of bytes read from the input stream so far
     */
    public long getCount() {
        return count.get();
    }

    public Copier execute() {
        if (this.future != null) {
            throw new IllegalStateException(logPrefix + "Already running");
        }
        this.future = ThreadPools.copyExecutor.submit(this);
        return this;
    }

    public void executeIfNotRunning() {
        if (future == null) {
            execute();
        }
    }

    /**
     * Closes and interrupts  the copier if necessary (This may be desired if it was detected that the receiver is no longer interested). And closes associated resources.
     */
    @Override
    public void close() throws IOException {
        if (cancelFutureIfNeeded()) {
            log.debug("Cancelled {}", future);
        }
        input.close();
        log.debug("closed");
    }

    /**
     * If running, cancel the job
     * @return {@code false} if the future is not running, or not yet running. {@code true} if there was a future to cancel, and it succeeded
     */
    boolean cancelFutureIfNeeded() {
        if (future != null) {
            if (future.isCancelled()) {
                log.debug("Future is cancelled already");
                return false;
            }
            if (future.isDone()) {
                log.debug("Future is done already");
                return false;
            }
            boolean result = future.cancel(true);
            if (! result) {
                log.debug("Couldn't cancel {}", future);
            }
            return result;
        }  else {
            return false;
        }
    }

    /**
     * After each batch, and at the end, we notify listeners.
     */
    private void notifyIfRequested() {
        if (notify != null) {
            log.trace("{}notifying listeners", logPrefix);
            synchronized (notify) {
                notify.notifyAll();
            }
        }
        synchronized (this) {
            this.notifyAll();
        }
    }

    private void handleError() {
        if (exception != null) {
             // The copier is ready, but resulted some error, the user requested to be called back about that, so do that now
            if (errorHandler != null) {
                try {
                    errorHandler.accept(this, exception);
                } catch (Exception e) {
                    log.error("Error during error handling: {}", e.getMessage(), e);
                    log.error("Error was {}", exception.getMessage(), exception);
                }
            } else {
                log.warn(exception.getMessage());
            }
        }
    }

    private void callBack() {
        if (callback != null) {
            log.debug("Calling back, {}", this);
            callback.accept(this);
            log.debug("Called back");
        }
    }

    @Override
    public String toString() {
        return logPrefix + super.toString();
    }
}
