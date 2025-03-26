package nl.vpro.monitoring.endpoints;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import nl.vpro.monitoring.web.WellKnownController;

@Configuration
public class WellknownEndpoints {

    @Bean
    public WellKnownController wellKnownController() {
        return new WellKnownController();
    }
}
