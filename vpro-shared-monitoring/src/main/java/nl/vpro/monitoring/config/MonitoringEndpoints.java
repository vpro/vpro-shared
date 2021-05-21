package nl.vpro.monitoring.config;

import io.micrometer.prometheus.PrometheusMeterRegistry;

import java.util.Collections;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerAdapter;

import nl.vpro.jackson2.Jackson2Mapper;
import nl.vpro.monitoring.web.HealthController;
import nl.vpro.monitoring.web.PrometheusController;

@Configuration
public class MonitoringEndpoints {

    @Bean
    public HealthController healthController() {
        return new HealthController();
    }

    @Bean
    public RequestMappingHandlerAdapter requestMappingHandlerAdapter() {
        final RequestMappingHandlerAdapter adapter = new RequestMappingHandlerAdapter();
        adapter.setMessageConverters(Collections.singletonList(new MappingJackson2HttpMessageConverter(Jackson2Mapper.getLenientInstance())));
        return adapter;
    }

    @Bean
    public PrometheusController prometheusController(PrometheusMeterRegistry registry) {
        return new PrometheusController(registry);
    }
}
