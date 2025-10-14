package nl.vpro.monitoring.web;

import io.micrometer.prometheusmetrics.PrometheusMeterRegistry;

import java.io.IOException;

import jakarta.servlet.FilterChain;

import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import nl.vpro.monitoring.config.MonitoringProperties;
import nl.vpro.monitoring.domain.Health;
import nl.vpro.monitoring.endpoints.ManageFilter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(SpringExtension.class)
@WebAppConfiguration
@ContextConfiguration(classes = TestSetup.class)
class PrometheusControllerTest {

    @Autowired
    private PrometheusMeterRegistry meterRegistry;

    @Autowired
    ManageFilter manageFilter;


    @BeforeEach
    public void setup() {

        meterRegistry.counter("test").increment();
    }

    @Test
    void initialValues() throws IOException {
        // Pragmatic testing of initial values. Using #wac and #mockMvc is more or less impossible.
        HealthController healthController = new HealthController();
        healthController.monitoringProperties = new MonitoringProperties();
        healthController.monitoringProperties.setUser("foo");
        healthController.monitoringProperties.setPassword("bar");
        healthController.monitoringProperties.setHealthPermitAll(true);
        healthController.request = new MockHttpServletRequest();
        healthController.response = new MockHttpServletResponse();
        ResponseEntity<Health> response = healthController.health();
        assertThat(response.getStatusCode().value()).isEqualTo(503);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().status()).isEqualTo(503);
        assertThat(response.getBody().message()).isEqualTo("Application starting");
    }

    @ParameterizedTest
    @ValueSource(strings = {"/prometheus", "/metrics"})
    void statusReady(String endpoint) throws Exception {

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Basic bWFuYWdlcjphZG1pbjJr");
        request.setContentType("text/plain");
        request.setServletPath("/manage" + endpoint);

        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = Mockito.mock(FilterChain.class);


        manageFilter.doFilter(request, response, chain);

        assertThat(response.getStatus()).isEqualTo(200);
        assertThat(response.getContentType()).isEqualTo("text/plain; version=0.0.4; charset=utf-8");

        assertThat(response.getContentAsString()).contains(
                """
                    # HELP test_total \s
                    # TYPE test_total counter
                    test_total\s""");
    }
}
