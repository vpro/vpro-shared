package nl.vpro.monitoring.endpoints;

import org.springframework.context.annotation.*;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;

import nl.vpro.monitoring.config.MonitoringProperties;

@Configuration
@Import({MonitoringEndpoints.class, WellKnownEndpoints.class})
public class Setup {

    @Bean
    public MonitoringProperties endpointMonitoringProperties() {
        return new MonitoringProperties();
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
        //om.registerModule(new JavaTim());
        om.setDefaultPropertyInclusion(JsonInclude.Include.NON_EMPTY);
        return om;
    }

}
