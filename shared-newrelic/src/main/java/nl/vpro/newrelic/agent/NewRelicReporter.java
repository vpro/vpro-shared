/**
 * Copyright (C) 2015 All rights reserved
 * VPRO The Netherlands
 */
package nl.vpro.newrelic.agent;

import java.util.Collection;
import java.util.List;

/**
 * @author Roelof Jan Koekoek
 * @since 0.22.0
 */
public interface NewRelicReporter {

    Collection<? extends Metric> getMetrics();

}
