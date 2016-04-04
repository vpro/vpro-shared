/**
 * Copyright (C) 2015 All rights reserved
 * VPRO The Netherlands
 */
package nl.vpro.newrelic.agent;

/**
 * @author Roelof Jan Koekoek
 * @since 0.22.0
 */
public interface NewRelicReporter {

    Iterable<? extends Metric> getMetrics();

}
