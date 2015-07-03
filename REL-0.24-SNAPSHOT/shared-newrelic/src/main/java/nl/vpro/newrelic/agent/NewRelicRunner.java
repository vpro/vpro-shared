/**
 * Copyright (C) 2015 All rights reserved
 * VPRO The Netherlands
 */
package nl.vpro.newrelic.agent;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.annotation.PostConstruct;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.newrelic.metrics.publish.Runner;

import nl.vpro.util.ThreadPools;

/**
 * @author Roelof Jan Koekoek
 * @since 0.22.0
 */
public class NewRelicRunner implements Runnable {
    private static final Logger LOG = LoggerFactory.getLogger(NewRelicRunner.class);

    private static final ExecutorService EXECUTOR =
        Executors.newSingleThreadExecutor(ThreadPools.createThreadFactory("NewRelicAgentRunner", true, Thread.MIN_PRIORITY));

    private List<NewRelicReporter> reporters;

    @PostConstruct
    public void start() {
        EXECUTOR.execute(this);
    }

    @Override
    public void run() {
        try {
            LOG.info("Starting an embedded NewRelic Agent");

            Runner runner = new Runner();
            runner.add(new NewRelicAgentFactory(this));
            runner.setupAndRun(); // Never returns
        } catch(Exception e) {
            LOG.error("Could not start the NewRelic Agent", e);
        }
    }

    List<NewRelicReporter> getReporters() {
        return reporters;
    }

    public void setReporters(List<NewRelicReporter> reporters) {
        this.reporters = reporters;
    }
}
