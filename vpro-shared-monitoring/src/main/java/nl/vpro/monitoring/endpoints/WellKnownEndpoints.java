package nl.vpro.monitoring.endpoints;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import nl.vpro.monitoring.web.WellKnownController;


/**
 * Just configures {@link WellKnownController} as a Spring bean.
 */
@Configuration
public class WellKnownEndpoints {

    @Bean
    public WellKnownController wellKnownController() {
        return new WellKnownController();
    }
}
