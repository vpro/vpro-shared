package nl.vpro.util;

import lombok.Builder;
import lombok.extern.slf4j.Slf4j;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.concurrent.Future;
import java.util.function.Consumer;

import org.apache.commons.io.IOUtils;

/**
* @author Michiel Meeuwissen
* @since 3.1
*/
@Slf4j
public class Copier implements Runnable {

    private boolean ready;
    private long count;
    private final InputStream in;
    private final OutputStream out;
    private final long batch;
    private final Consumer<Copier> callback;
    private final Consumer<Copier> batchConsumer;
    private Future<?> future;


    public Copier(InputStream i, OutputStream o, long batch) {
        this(i, o, batch, null, null, 0);
    }

    @Builder
    private Copier(
        InputStream input,
        OutputStream output,
        long batch,
        Consumer<Copier> batchConsumer,
        Consumer<Copier> callback,
        int offset
        ) {
        in = input;
        out = output;
        this.batch = batch == 0 ? 8192: batch;
        this.callback = callback;
        this.batchConsumer = batchConsumer;
        this.count = offset;
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
                    if (Thread.currentThread().isInterrupted()) {
                        break;
                    }
                }
            }
        } catch (Throwable t) {
            log.error("Connector " + toString() + ": " + t.getClass() + " " + t.getMessage());
        }
        synchronized (this) {
            ready = true;
            if (callback != null) {
                callback.accept(this);
            }
            log.debug("notifying listeners");
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
            throw new IllegalStateException("Already running");
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

    public boolean interrupt() {
        return future != null && future.cancel(true);
    }

}
