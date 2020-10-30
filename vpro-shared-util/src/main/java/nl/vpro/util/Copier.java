package nl.vpro.util;

import lombok.Getter;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

import java.io.*;
import java.util.Optional;
import java.util.concurrent.Future;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import org.apache.commons.io.IOUtils;

/**
 * Can be used to copy an {@link InputStream} to an {@link OutputStream} in a stand alone thread.
 * Used by {@link FileCachingInputStream}.
 *
 * @author Michiel Meeuwissen
 * @since 3.1
 */
@Slf4j
@ToString
public class Copier implements Runnable, Closeable {

    private boolean ready;
    private Throwable exception;
    private long count;

    private final InputStream input;
    private final OutputStream output;
    private final long batch;
    private final Consumer<Copier> callback;
    private final BiConsumer<Copier, Throwable> errorHandler;
    private final Consumer<Copier> batchConsumer;
    @Getter
    private Future<?> future;
    private final String name;
    private final Object notify;


    public Copier(InputStream i, OutputStream o, long batch) {
        this(i, o, batch, null, null, null,  0, null, null);
    }


    /**
     *
     * @param input The input stream to copy from (will be closed if ready)
     * @param output The output stream to copy to (will not be implicetely closed)
     * @param batch
     * @param batchConsumer
     * @param callback
     * @param errorHandler
     * @param offset
     * @param name
     * @param notify if a batch was handled notify this object
     */
    @lombok.Builder(builderClassName = "Builder")
    private Copier(
        InputStream input,
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
        this.output = output;
        this.batch = batch == 0 ? 8192: batch;
        this.callback = callback;
        this.batchConsumer = batchConsumer;
        this.errorHandler = errorHandler;
        this.count = offset;
        this.name = name;
        this.notify = notify;
    }

    public Copier(InputStream i, OutputStream o) {
        this(i, o, 8192L);
    }

    @Override
    public void run() {
        try {
            if (batchConsumer == null || batch < 1) {
                count += IOUtils.copyLarge(input, output);
                notifyIfRequested();
            } else {
                batchConsumer.accept(this);
                while (true) {
                    long result = IOUtils.copyLarge(input, output, 0, batch);
                    count += result;
                    batchConsumer.accept(this);
                    notifyIfRequested();
                    if (result < batch) {
                        break;
                    }
                }
            }
        } catch (IOException ioe) {
            exception = ioe;
            log.debug(ioe.getMessage());
            notifyIfRequested();
        } catch (Throwable t) {
            if (! CommandExecutor.isBrokenPipe(t)) {
                log.warn("{}Connector {}\n{} {}", logPrefix(), toString(), t.getClass().getName(), t.getMessage());
            }
            exception = t;
            notifyIfRequested();
        } finally {
            log.debug("Ready");
            synchronized (this) {
                ready = true;
                handleError();
            }
            callBack();
        }
    }

    public void waitFor() throws InterruptedException {
        executeIfNotRunning();
        synchronized (this) {
            while (!ready) {
                wait();
            }
        }
    }

    public void waitForAndClose() throws InterruptedException, IOException {
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
        if (ready) {
            throwIOExceptionIfNeeded();
        }
        return ready;
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
        return count;
    }

    public Copier execute() {
        if (future != null) {
            throw new IllegalStateException(logPrefix() + "Already running");
        }
        future = ThreadPools.copyExecutor.submit(this);
        return this;
    }

    public Copier executeIfNotRunning() {
        if (future == null) {
            execute();
        }
        return this;
    }

    @Override
    public void close() throws IOException {
        input.close();
        if (future != null) {
            future.cancel(false);
            future = null;
        }
    }

    /**
     * Interrupts the copier. This may be desired if it was detected that the receiver is no longer interested.
     *
     * @return True if any interruption happened. False if the future was canceled or done already, or if cancelling failed.
     * @throws IOException
     */

    public boolean interrupt() throws IOException {
        try {
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
                    log.warn("Couldn't cancel {}", future);
                }
                return result;
            }  else {
                return ! ready;
            }
        } finally {
            close();
        }
    }

    protected String logPrefix() {
        return name == null ? "" : name + ": ";
    }


    private void notifyIfRequested() {
          if (notify != null) {
              synchronized (notify) {
                  notify.notifyAll();
              }
          }
    }

    private void handleError() {
        if (errorHandler != null && exception != null) {
             // The copier is ready, but resulted some error, the user requested to be called back, so do that now
            log.debug("Error handling");
            try {
                errorHandler.accept(this, exception);
            } catch(Exception e) {
                log.error("Error during error handling: {}", e.getMessage(), e);
                log.error("Error was {}", exception.getMessage(), exception);
            }
        }
    }

    private void callBack() {
        if (callback != null) {
            log.debug("Calling back");
            callback.accept(this);
        }
        synchronized (this) {
            log.debug("{}notifying listeners", logPrefix());
            // ready now, notify threads waiting
            notifyAll();
        }
    }
}
