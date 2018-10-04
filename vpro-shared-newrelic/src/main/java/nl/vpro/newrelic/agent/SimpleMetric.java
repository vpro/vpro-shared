/*
 * Copyright (C) 2015 All rights reserved
 * VPRO The Netherlands
 */
package nl.vpro.newrelic.agent;

import lombok.Getter;
import lombok.Setter;

/**
 * @author Roelof Jan Koekoek
 * @since 0.24.0
 */
public class SimpleMetric implements Metric {

    @Getter
    private final String name;

    @Getter
    private final String unit;

    @Getter
    @Setter
    private float value;

    public SimpleMetric(String name, String unit) {
        this.name = name;
        this.unit = unit;
    }

    @Override
    public String toString() {
        return name + " = " + value + " " + unit;
    }
}
