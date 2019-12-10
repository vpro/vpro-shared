/*
 * Copyright (C) 2015 All rights reserved
 * VPRO The Netherlands
 */
package nl.vpro.newrelic.agent;


import lombok.extern.slf4j.Slf4j;

import java.util.List;

import com.newrelic.metrics.publish.Agent;

/**
 * @author Roelof Jan Koekoek
 * @since 0.22.0
 */
@Slf4j
public class NewRelicAgent extends Agent {

    private final String env;

    private final List<NewRelicReporter> reporters;

    public NewRelicAgent(String GUID, String env, String version, List<NewRelicReporter> reporters) {
        super(GUID, version);
        this.env = env;
        this.reporters = reporters;
    }

    @Override
    public void pollCycle() {
        for(NewRelicReporter reporter : reporters) {
            for(Metric metric : reporter.getMetrics()) {
                reportMetric(metric.getName(), metric.getUnit(), metric.getValue());
            }
        }
    }

    @Override
    public String getAgentName() {
        return env;
    }
}
