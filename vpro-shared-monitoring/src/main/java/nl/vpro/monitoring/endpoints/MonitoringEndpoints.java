package nl.vpro.monitoring.endpoints;

import io.micrometer.prometheusmetrics.PrometheusMeterRegistry;

import jakarta.inject.Named;
import jakarta.inject.Provider;

import org.springframework.context.annotation.*;

import nl.vpro.monitoring.config.MonitoringProperties;
import nl.vpro.monitoring.web.HealthController;
import nl.vpro.monitoring.web.PrometheusController;

@Configuration
public class MonitoringEndpoints {



    @Bean
    @Lazy
    public HealthController healthController() {
        return new HealthController();
    }

    @Bean
    @Lazy
    public PrometheusController prometheusController(Provider<PrometheusMeterRegistry> registry, @Named("endpointMonitoringProperties") MonitoringProperties properties) {
        return new PrometheusController(registry, properties);
    }

}
