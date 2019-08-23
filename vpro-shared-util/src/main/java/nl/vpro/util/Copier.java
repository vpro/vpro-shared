package nl.vpro.util;

import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
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
    private long count;

    private final InputStream input;
    private final OutputStream output;
    private final long batch;
    private final Consumer<Copier> callback;
    private final BiConsumer<Copier, Throwable> errorHandler;
    private final Consumer<Copier> batchConsumer;
    private Future<?> future;
    private final String name;

    private final Object notify;


    public Copier(InputStream i, OutputStream o, long batch) {
        this(i, o, batch, null, null, null,  0, null, null);
    }

    /**
     *
     * @param input The input stream to copy from (will be close if ready)
     * @param output The output stream to copy to (will not be implicetely closed)
     * @param batch
     * @param batchConsumer
     * @param callback
     * @param errorHandler
     * @param offset
     * @param name
     * @param notify
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
                if (notify != null) {
                    synchronized (notify) {
                        notify.notifyAll();
                    }
                }
            } else {
                batchConsumer.accept(this);
                while (true) {
                    long result = IOUtils.copyLarge(input, output, 0, batch);
                    count += result;
                    if (notify != null) {
                        synchronized (notify) {
                            notify.notifyAll();
                        }
                    }

                    if (result < batch) {
                        break;
                    }
                    batchConsumer.accept(this);
                }
            }
        } catch (Throwable t) {
            if (! CommandExecutor.isBrokenPipe(t)) {
                log.warn("{}Connector " + toString() + ": " + t.getClass() + " " + t.getMessage(), logPrefix());
            }
            if (errorHandler != null) {
                errorHandler.accept(this, t);
            }
        } finally {
            synchronized (this) {
                ready = true;
                if (callback != null) {
                    callback.accept(this);
                }
                log.debug("{}notifying listeners", logPrefix());
                notifyAll();
            }
        }
    }

    public void waitFor() throws InterruptedException {
        synchronized (this) {
            while (!ready) {
                wait();
            }
        }
    }

    public boolean isReady() {
        return ready;
    }


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
     *
     * @return True if any interruption happened. False if the future was canceled or done already, or if cancellng failed.
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
                future.cancel(true);
                boolean result = future.cancel(true);
                if (! result) {
                    log.warn("Couldn't cancel {}", future);
                }
                return result;
            }

            return true;
        } finally {
            close();
        }
    }

    protected String logPrefix() {
        return name == null ? "" : name + ": ";
    }

}
