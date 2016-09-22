package nl.vpro.util;

import lombok.Builder;

import java.io.InputStream;
import java.io.OutputStream;

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

    private final Logger log;
    private final String tempFile;
    private final long batch;
    private final Runnable afterReady;


    public Copier(InputStream i, OutputStream o, Logger log, String tempFile, long batch) {
        this(i, o, log, tempFile, batch, () -> {
        });
    }

    @Builder
    private Copier(InputStream input, OutputStream output, Logger log, String tempFile, long batch, Runnable afterReady) {
        in = input;
        out = output;
        this.log = log;
        this.tempFile = tempFile;
        this.batch = batch == 0 ? 8192: batch; 
        this.afterReady = afterReady;
    }
    
    public Copier(InputStream i, OutputStream o) {
        this(i, o, null, null, 0L);
    }

    @Override
    public void run() {
        try {
            if (log == null) {
                count = IOUtils.copyLarge(in, out);
            } else {
                // Download changes locally before streaming them to avoid network timeouts
                count = 0;
                while (true) {
                    long result = IOUtils.copyLarge(in, out, 0, batch);
                    count += result;
                    if (result < batch) {
                        if (count > batch) {
                            log.info("Created {} ({} bytes written)", tempFile, count);
                        } else {
                            log.debug("Created {} ({} bytes written)", tempFile, count);
                        }
                        break;
                    } else {
                        log.info("Creating {} ({} bytes written)", tempFile, count);
                    }
                }
            }
        } catch (Throwable t) {
            LOG.error("Connector " + toString() + ": " + t.getClass() + " " + t.getMessage());
        }
        synchronized (this) {
            ready = true;
            if (afterReady != null) {
                afterReady.run();
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

}
