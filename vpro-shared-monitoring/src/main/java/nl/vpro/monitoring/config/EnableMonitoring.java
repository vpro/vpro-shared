package nl.vpro.monitoring.config;

import java.lang.annotation.*;

import org.springframework.context.annotation.Import;


/**
 *This annotation can be used to bootstrap monitoring configuration as defined in {@link MeterRegistryConfiguration}
 */
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Import({MeterRegistryConfiguration.class})
public @interface EnableMonitoring {
}
