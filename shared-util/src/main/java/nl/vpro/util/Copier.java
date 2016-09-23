package nl.vpro.util;

import lombok.Builder;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.function.Consumer;

import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
* @author Michiel Meeuwissen
* @since 3.1
*/
public class Copier implements Runnable {

    private static final Logger LOG = LoggerFactory.getLogger(Copier.class);

    private boolean ready;
    private long count = 0;
    private final InputStream in;
    private final OutputStream out;
    private final long batch;
    private final Consumer<Copier> callback;
    private final Consumer<Copier> batchConsumer;


    public Copier(InputStream i, OutputStream o, long batch) {
        this(i, o, batch, null, null);
    }

    @Builder
    private Copier(
        InputStream input,
        OutputStream output,
        long batch,
        Consumer<Copier> batchConsumer,
        Consumer<Copier> callback) {
        in = input;
        out = output;
        this.batch = batch == 0 ? 8192: batch;
        this.callback = callback;
        this.batchConsumer = batchConsumer;
    }

    public Copier(InputStream i, OutputStream o) {
        this(i, o, 8192L);
    }

    @Override
    public void run() {
        try {
            if (batchConsumer == null || batch < 1) {
                count = IOUtils.copyLarge(in, out);
            } else {
                count = 0;
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
            LOG.error("Connector " + toString() + ": " + t.getClass() + " " + t.getMessage());
        }
        synchronized (this) {
            ready = true;
            if (callback != null) {
                callback.accept(this);
            }
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
        ThreadPools.copyExecutor.execute(this);
        return this;
    }

}
