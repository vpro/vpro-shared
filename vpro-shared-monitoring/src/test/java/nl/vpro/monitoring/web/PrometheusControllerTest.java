package nl.vpro.monitoring.web;

import io.micrometer.prometheus.PrometheusMeterRegistry;

import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import nl.vpro.monitoring.domain.Health;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(SpringExtension.class)
@WebAppConfiguration
@ContextConfiguration(value = "/manage-servlet.xml")
class PrometheusControllerTest {

    @Autowired
    private PrometheusMeterRegistry meterRegistry;

    @Autowired
    private WebApplicationContext wac;

    private MockMvc mockMvc;

    @BeforeEach
    public void setup() {
        this.mockMvc = MockMvcBuilders.webAppContextSetup(this.wac).build();
        meterRegistry.counter("test").increment();
    }

    @Test
    void initialValues() {
        // Pragmatic testing of initial values. Using #wac and #mockMvc is more or less impossible.
        ResponseEntity<Health> response = new HealthController().health();
        assertThat(response.getStatusCodeValue()).isEqualTo(503);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getStatus()).isEqualTo(503);
        assertThat(response.getBody().getMessage()).isEqualTo("Application starting");
    }

    @Test
    void statusReady() throws Exception {
        mockMvc.perform(
            get("/prometheus")
                .accept("text/plain")
        ).andExpect(status().is(200))
            .andExpect(content().contentType("text/plain; version=0.0.4; charset=utf-8"))
            .andExpect(content().string(Matchers.containsString(
                "# HELP test_total  \n" +
                    "# TYPE test_total counter\n" +
                    "test_total 1.0\n")));
    }
}
