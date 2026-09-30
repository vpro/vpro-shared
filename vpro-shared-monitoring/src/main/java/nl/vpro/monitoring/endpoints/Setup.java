package nl.vpro.monitoring.endpoints;

import tools.jackson.databind.ObjectWriter;
import tools.jackson.databind.json.JsonMapper;

import org.springframework.context.annotation.*;

import com.fasterxml.jackson.annotation.JsonInclude;

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
    public ObjectWriter monitoringObjectWriter() {
        JsonMapper.Builder builder = JsonMapper.builder();
        builder.changeDefaultPropertyInclusion(v -> v.withValueInclusion(JsonInclude.Include.NON_EMPTY));
        return builder.build().writer();
    }

}
