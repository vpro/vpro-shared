/**
 * Copyright (C) 2015 All rights reserved
 * VPRO The Netherlands
 */
package nl.vpro.newrelic.agent;

/**
 * @author Roelof Jan Koekoek
 * @since 3.7
 */
public interface Metric {

    /**
     * Get the name of this metric i.e. "Database/Connections". NewRelic prefixes all names at runtime
     * with "Component/". The metric name in
     *
     * @return name
     */
    String getName();

    /**
     * Get the unit of this metric i.e. "bytes/sec" or "seconds|call". NewRelic appends the unit to the
     * metric name enclosing them in square brackets.
     *
     * See https://docs.newrelic.com/docs/plugins/plugin-developer-resources/developer-reference/metric-units-reference
     *
     * @return name
     */
    String getUnit();

    /**
     * The metric value.
     *
     * @return
     */
    float getValue();
}
