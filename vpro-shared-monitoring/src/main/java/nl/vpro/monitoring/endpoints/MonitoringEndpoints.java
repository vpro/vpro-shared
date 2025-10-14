package nl.vpro.monitoring.endpoints;

import io.micrometer.prometheusmetrics.PrometheusMeterRegistry;

import java.util.Optional;

import jakarta.inject.Named;
import jakarta.inject.Provider;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import nl.vpro.monitoring.config.MonitoringProperties;
import nl.vpro.monitoring.web.HealthController;
import nl.vpro.monitoring.web.PrometheusController;

@Configuration
public class MonitoringEndpoints {



    @Bean
    public HealthController healthController() {
        return new HealthController();
    }

    @Bean
    public PrometheusController prometheusController(Provider<PrometheusMeterRegistry> registry, @Named("endpointMonitoringProperties") MonitoringProperties properties) {
        return new PrometheusController(registry, properties);
    }

}
