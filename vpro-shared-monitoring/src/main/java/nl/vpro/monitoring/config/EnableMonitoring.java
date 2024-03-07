package nl.vpro.monitoring.config;

import org.springframework.context.annotation.Import;

import java.lang.annotation.*;


/**
 *This annotation can be used to bootstrap monitoring configuration as defined in {@link MonitoringConfig}
 */
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Import({MonitoringConfig.class})
public @interface EnableMonitoring {
}
