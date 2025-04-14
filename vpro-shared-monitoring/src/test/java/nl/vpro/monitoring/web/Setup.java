package nl.vpro.monitoring.web;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.ImportResource;

import nl.vpro.monitoring.config.EnableMonitoring;

@Configuration
@EnableMonitoring
@ImportResource("classpath:/META-INF/manage-servlet.xml")

public class Setup {
}
