/*
 * Copyright (C) 2015 All rights reserved
 * VPRO The Netherlands
 */
package nl.vpro.newrelic.agent;

import lombok.Getter;

import java.util.concurrent.atomic.AtomicInteger;

import com.google.common.util.concurrent.AtomicDouble;

/**
 * @author Roelof Jan Koekoek
 * @since 0.24.0
 */
public class AverageMetric implements Metric {

    @Getter
    private final String name;

    @Getter
    private final String unit;

    private final AtomicDouble complement = new AtomicDouble();

    private AtomicInteger counts = new AtomicInteger();

    public AverageMetric(String name, String unit) {
        this.name = name;
        this.unit = unit;
    }


    @Override
    public float getValue() {
        if(counts.get() == 0) {
            return 0;
        }

        return (float)complement.getAndSet(0d) / counts.getAndSet(0);
    }

    public void addValue(float value) {
        counts.incrementAndGet();
        this.complement.addAndGet(value);
    }

    @Override
    public String toString() {
        return name + "=" + getValue() + " " + unit;
    }
}
