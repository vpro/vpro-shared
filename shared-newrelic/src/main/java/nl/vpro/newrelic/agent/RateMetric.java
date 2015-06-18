/**
 * Copyright (C) 2015 All rights reserved
 * VPRO The Netherlands
 */
package nl.vpro.newrelic.agent;

import java.util.concurrent.TimeUnit;

/**
 * @author Roelof Jan Koekoek
 * @since 0.22.0
 */
public class RateMetric implements Metric {
    private final String name;

    private final String unit;

    private final TimeUnit period;

    private final RateCounter counter = new RateCounter();

    public RateMetric(String name, String unit, TimeUnit period) {
        this.name = name;
        this.unit = unit;
        this.period = period;
    }

    public void increment() {
        counter.increment();
    }

    public String getName() {
        return name;
    }

    public String getUnit() {
        return unit;
    }

    public float getValue() {
        return counter.getRatio(period);
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("WindowedMetric{");
        sb.append("name='").append(name).append('\'');
        sb.append(", unit='").append(unit).append('\'');
        sb.append(", period=").append(period);
        sb.append(", counter=").append(counter);
        sb.append('}');
        return sb.toString();
    }
}
