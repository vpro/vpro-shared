package nl.vpro.monitoring.endpoints;

import io.micrometer.prometheusmetrics.PrometheusMeterRegistry;

import java.util.Collections;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerAdapter;

import com.fasterxml.jackson.databind.SerializationFeature;

import nl.vpro.jackson2.DateModule;
import nl.vpro.jackson2.Jackson2Mapper;
import nl.vpro.monitoring.web.HealthController;
import nl.vpro.monitoring.web.PrometheusController;

@Configuration
public class MonitoringEndpoints {


    @Bean
    public RequestMappingHandlerAdapter requestMappingHandlerAdapter() {
        final RequestMappingHandlerAdapter adapter = new RequestMappingHandlerAdapter();
        adapter.setMessageConverters(
            Collections.singletonList(new MappingJackson2HttpMessageConverter(
                Jackson2Mapper.create("monitoring", m -> !(m instanceof DateModule), om -> {
                    om.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
                    om.disable(SerializationFeature.WRITE_DURATIONS_AS_TIMESTAMPS);

                })
            )));

        return adapter;
    }

    @Bean
    public HealthController healthController() {
        return new HealthController();
    }

    @Bean
    public PrometheusController prometheusController(PrometheusMeterRegistry registry) {
        return new PrometheusController(registry);
    }
}
