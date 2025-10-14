package nl.vpro.monitoring.endpoints;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import nl.vpro.monitoring.config.MonitoringProperties;

import org.springframework.context.annotation.*;

@Configuration
@Import({MonitoringEndpoints.class, WellKnownEndpoints.class})
public class Setup {

    @Bean
    public MonitoringProperties endpointMonitoringProperties() {
        MonitoringProperties monitoringProperties = new MonitoringProperties();
        return monitoringProperties;
    }

    @Bean
    public ManageFilter manageFilter() {
        return new ManageFilter();
    }

    @Bean
    public ObjectMapper monitoringObjectMapper() {
        ObjectMapper om = new ObjectMapper() {
            @Override
            public String toString() {
                return "ObjectMapper(monitoring)";
            }
        };
        om.registerModule(new JavaTimeModule());
        om.setDefaultPropertyInclusion(JsonInclude.Include.NON_EMPTY);
        return om;
    }

}
