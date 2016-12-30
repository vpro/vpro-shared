/*
 * Copyright (C) 2016 All rights reserved
 * VPRO The Netherlands
 */
package nl.vpro.util;

import java.io.IOException;
import java.io.InputStream;
import java.time.Duration;

/**
 * @author rico
 */
public class DelayedInputStream extends InputStream {
    private final InputStream source;

    private final long delay;

    public DelayedInputStream(InputStream source, Duration delay) {
        this.source = source;
        this.delay = delay.toMillis();
    }

    @Override
    public int read() throws IOException {
        try {
            Thread.sleep(delay);
        } catch (InterruptedException ignored) {
        }
        return source.read();
    }
}
