/**
 * Copyright (C) 2015 All rights reserved
 * VPRO The Netherlands
 */
package nl.vpro.newrelic.agent;

import java.util.concurrent.atomic.AtomicInteger;

import com.google.common.util.concurrent.AtomicDouble;

/**
 * @author Roelof Jan Koekoek
 * @since 0.24.0
 */
public class AverageMetric implements Metric {

    private final String name;

    private final String unit;

    private final AtomicDouble complement = new AtomicDouble();

    private AtomicInteger counts = new AtomicInteger();

    public AverageMetric(String name, String unit) {
        this.name = name;
        this.unit = unit;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String getUnit() {
        return unit;
    }

    @Override
    public float getValue() {
        return (float)complement.getAndSet(0d) / counts.getAndSet(0);
    }

    public void addValue(float value) {
        this.complement.addAndGet(value);
    }
}
