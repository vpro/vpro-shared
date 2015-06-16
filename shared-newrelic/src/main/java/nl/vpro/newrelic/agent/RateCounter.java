/**
 * Copyright (C) 2015 All rights reserved
 * VPRO The Netherlands
 */
package nl.vpro.newrelic.agent;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author Roelof Jan Koekoek
 * @since 3.7
 */
public class RateCounter {

    private int previousCount = 0;

    private long previousCountTimestamp = System.currentTimeMillis();

    private AtomicInteger count = new AtomicInteger(0);

    public void increment() {
        count.incrementAndGet();
    }

    public float getRatio(TimeUnit unit) {
        // Not fully synchronised, but cheaper and should suffice
        final long now = System.currentTimeMillis();
        final int value = count.intValue();

        final long period = unit.convert(now - previousCountTimestamp, TimeUnit.MILLISECONDS);
        if(period == 0) {
            // truncated to zero don't shift
            return 0;
        }

        float ratio = (float)(value - previousCount) / period;

        previousCount = value;
        previousCountTimestamp = now;

        return ratio;
    }


}
