/*
 * Copyright (C) 2015 All rights reserved
 * VPRO The Netherlands
 */
package nl.vpro.newrelic.agent;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

import com.newrelic.metrics.publish.Runner;

import nl.vpro.util.ThreadPools;

/**
 * @author Roelof Jan Koekoek
 * @since 0.22.0
 */
@Slf4j
public class NewRelicRunner implements Runnable {

    private static final ExecutorService EXECUTOR =
        Executors.newSingleThreadExecutor(ThreadPools.createThreadFactory("NewRelicAgentRunner", true, Thread.MIN_PRIORITY));

    @Getter
    @Setter
    private List<NewRelicReporter> reporters;

    @PostConstruct
    public void start() {
        EXECUTOR.execute(this);
    }

    @PreDestroy
    public void shutdown() {
        EXECUTOR.shutdownNow();
    }

    @Override
    public void run() {
        try {
            log.info("Starting an embedded NewRelic Agent");

            Runner runner = new Runner();
            runner.add(new NewRelicAgentFactory(this));
            runner.setupAndRun(); // Never returns
        } catch(Exception e) {
            log.error("Could not start the NewRelic Agent", e);
        }
    }

}
