package nl.vpro.monitoring.endpoints;

import io.micrometer.prometheusmetrics.PrometheusMeterRegistry;

import java.util.Collections;
import java.util.Optional;

import jakarta.inject.Named;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerAdapter;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import nl.vpro.monitoring.config.MonitoringProperties;
import nl.vpro.monitoring.web.HealthController;
import nl.vpro.monitoring.web.PrometheusController;

@Configuration
public class MonitoringEndpoints {

    @Bean
    public RequestMappingHandlerAdapter requestMappingHandlerAdapter() {
        final RequestMappingHandlerAdapter adapter = new RequestMappingHandlerAdapter();
        ObjectMapper om = new ObjectMapper() {
            @Override
            public String toString() {
                return "ObjectMapper(monitoring)";
            }
        };
        om.registerModule(new JavaTimeModule());
        om.setDefaultPropertyInclusion(JsonInclude.Include.NON_EMPTY);

        adapter.setMessageConverters(
            Collections.singletonList(new MappingJackson2HttpMessageConverter(om)));

        return adapter;
    }

    @Bean
    public HealthController healthController() {
        return new HealthController();
    }

    @Bean
    public PrometheusController prometheusController(Optional<PrometheusMeterRegistry> registry, @Named("endpointMonitoringProperties") MonitoringProperties properties) {
        return new PrometheusController(registry, properties);
    }

}
