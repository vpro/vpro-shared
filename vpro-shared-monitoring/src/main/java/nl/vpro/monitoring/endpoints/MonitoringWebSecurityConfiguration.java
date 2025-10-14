package nl.vpro.monitoring.endpoints;

import nl.vpro.monitoring.web.Authentication;

import org.springframework.context.annotation.Configuration;


/**
 * @deprecated Not defining any beans anymore. Authentication is now arranged without spring security, just from the calls in {@link nl.vpro.monitoring.web.HealthController} and {@link nl.vpro.monitoring.web.PrometheusController} themselves.
 * @see Authentication
 */
@Deprecated
public class MonitoringWebSecurityConfiguration {


}
