package nl.vpro.util;

import lombok.Builder;
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
* @author Michiel Meeuwissen
* @since 3.1
*/
@Slf4j
@ToString
public class Copier implements Runnable, Closeable {

    private boolean ready;
    private long count;
    private final InputStream in;
    private final OutputStream out;
    private final long batch;
    private final Consumer<Copier> callback;
    private final BiConsumer<Copier, Throwable> errorHandler;
    private final Consumer<Copier> batchConsumer;
    private Future<?> future;
    private final String name;


    public Copier(InputStream i, OutputStream o, long batch) {
        this(i, o, batch, null, null, null,  0, null);
    }

    @Builder
    private Copier(
        InputStream input,
        OutputStream output,
        long batch,
        Consumer<Copier> batchConsumer,
        Consumer<Copier> callback,
        BiConsumer<Copier, Throwable> errorHandler,
        int offset,
        String name
        ) {
        in = input;
        out = output;
        this.batch = batch == 0 ? 8192: batch;
        this.callback = callback;
        this.batchConsumer = batchConsumer;
        this.errorHandler = errorHandler;
        this.count = offset;
        this.name = name;
    }

    public Copier(InputStream i, OutputStream o) {
        this(i, o, 8192L);
    }

    @Override
    public void run() {
        try {
            if (batchConsumer == null || batch < 1) {
                count += IOUtils.copyLarge(in, out);
            } else {
                batchConsumer.accept(this);
                while (true) {
                    long result = IOUtils.copyLarge(in, out, 0, batch);
                    count += result;
                    if (result < batch) {
                        break;
                    }
                    batchConsumer.accept(this);
                }
            }
        } catch (Throwable t) {
            if (! CommandExecutor.isBrokenPipe(t)) {
                log.error("{}Connector " + toString() + ": " + t.getClass() + " " + t.getMessage(), logPrefix());
            }
            if (errorHandler != null) {
                errorHandler.accept(this, t);
            }
        }
        synchronized (this) {
            ready = true;
            if (callback != null) {
                callback.accept(this);
            }
            log.debug("{}notifying listeners", logPrefix());
            notifyAll();
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
        in.close();
    }

    public boolean interrupt() throws IOException {
        close();
        return future != null && future.cancel(true);
    }

    protected String logPrefix() {
        return name == null ? "" : name + ": ";
    }

}
