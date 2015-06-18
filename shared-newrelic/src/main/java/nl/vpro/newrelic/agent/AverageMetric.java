/**
 * Copyright (C) 2015 All rights reserved
 * VPRO The Netherlands
 */
package nl.vpro.newrelic.agent;

/**
 * @author Roelof Jan Koekoek
 * @since 0.24.0
 */
public class AverageMetric implements Metric {

    private final String name;

    private final String unit;

    private float complement;

    private int counts;

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
        final float answer = complement / counts;
        complement = counts = 0;
        return answer;
    }

    public void addValue(float value) {
        this.complement += value;
    }
}
