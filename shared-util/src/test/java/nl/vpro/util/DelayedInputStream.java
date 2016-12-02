/*
 * Copyright (C) 2016 All rights reserved
 * VPRO The Netherlands
 */
package nl.vpro.util;

import java.io.IOException;
import java.io.InputStream;

/**
 * @author rico
 * @date 05/10/2016
 */
public class DelayedInputStream extends InputStream {
    InputStream source;

    long delay;

    public DelayedInputStream(InputStream source, long delay) {
        this.source = source;
        this.delay = delay;
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
