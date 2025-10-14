package nl.vpro.monitoring.endpoints;

import org.springframework.context.annotation.*;

import nl.vpro.monitoring.web.WellKnownController;


/**
 * Just configures {@link WellKnownController} as a Spring bean.
 */
@Configuration
public class WellKnownEndpoints {

    @Bean
    @Lazy
    public WellKnownController wellKnownController() {
        return new WellKnownController();
    }
}
