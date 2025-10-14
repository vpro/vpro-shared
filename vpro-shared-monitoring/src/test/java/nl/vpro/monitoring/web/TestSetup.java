package nl.vpro.monitoring.web;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import nl.vpro.monitoring.config.EnableMonitoring;
import nl.vpro.monitoring.endpoints.Setup;

@Configuration
@EnableMonitoring
@Import(Setup.class)
public class TestSetup {
}
