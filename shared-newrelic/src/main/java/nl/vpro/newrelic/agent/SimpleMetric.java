/**
 * Copyright (C) 2015 All rights reserved
 * VPRO The Netherlands
 */
package nl.vpro.newrelic.agent;

/**
 * @author Roelof Jan Koekoek
 * @since 0.24.0
 */
public class SimpleMetric implements Metric {

    private final String name;

    private final String unit;

    private float value;

    public SimpleMetric(String name, String unit) {
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
        return value;
    }

    public void setValue(float value) {
        this.value = value;
    }
}
