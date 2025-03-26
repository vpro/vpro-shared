package nl.vpro.monitoring.config;

import java.lang.annotation.*;

import org.springframework.context.annotation.Import;


/**
 *This annotation can be used to bootstrap monitoring configuration as defined in {@link MonitoringWebSecurityConfiguration}
 * @since 5.0
 */
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Import({MonitoringWebSecurityConfiguration.class})
public @interface EnableMonitoringWebSecurity {
}
