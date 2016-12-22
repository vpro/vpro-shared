/*
 * Copyright (C) 2015 All rights reserved
 * VPRO The Netherlands
 */
package nl.vpro.newrelic.agent;

import java.util.Map;

import com.newrelic.metrics.publish.Agent;
import com.newrelic.metrics.publish.AgentFactory;
import com.newrelic.metrics.publish.configuration.ConfigurationException;

/**
 * @author Roelof Jan Koekoek
 * @since 0.22.0
 */
public class NewRelicAgentFactory extends AgentFactory {

    private final NewRelicRunner runner;

    public NewRelicAgentFactory(NewRelicRunner runner) {
        this.runner = runner;
    }

    @Override
    public Agent createConfiguredAgent(Map<String, Object> properties) throws ConfigurationException {
        String plugin = (String)properties.get("plugin");
        if(plugin == null) {
            plugin = "Installed plugin not set";
        }

        String env = (String)properties.get("env");
        if(env == null) {
            env = "Installed environment not set";
        }

        String version = (String)properties.get("version");
        if(version == null) {
            version = "1.0.0";
        }

        return new NewRelicAgent(plugin, env, version, runner.getReporters());
    }
}
