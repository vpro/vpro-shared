package nl.vpro.monitoring.config;

import org.springframework.context.annotation.Import;

import java.lang.annotation.*;


import nl.vpro.monitoring.endpoints.MonitoringWebSecurityConfiguration;


/**
 *This annotation can be used to bootstrap monitoring configuration as defined in {@link MonitoringWebSecurityConfiguration}
 * @since 5.0
 */
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Import({MonitoringWebSecurityConfiguration.class})
public @interface EnableMonitoringWebSecurity {
}
