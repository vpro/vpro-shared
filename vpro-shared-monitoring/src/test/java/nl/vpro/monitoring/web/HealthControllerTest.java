package nl.vpro.monitoring.web;

import io.micrometer.prometheusmetrics.PrometheusConfig;
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry;

import java.time.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.meeuw.math.time.TestClock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.is;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(SpringExtension.class)
@WebAppConfiguration
@ContextConfiguration("/manage-servlet.xml")
class HealthControllerTest {

    @Autowired
    private HealthController healthController;

    @Autowired
    private WebApplicationContext wac;

    private MockMvc mockMvc;

    private final TestClock clock = new TestClock(ZoneId.of("UT"), Instant.parse("2021-07-06T00:00:00.000Z"));


    @BeforeEach
    public void setup() {
        this.healthController.clock = clock;
        this.healthController.prometheusController = new PrometheusController(new PrometheusMeterRegistry(new PrometheusConfig() {
            @Override
            public String get(String s) {
                return null;
            }
        }));
        this.mockMvc = MockMvcBuilders.webAppContextSetup(this.wac).build();
        this.healthController.markReady(null);
    }


    @Test
    void statusReady() throws Exception {
        healthController.onApplicationStartedEvent(null);
        clock.tick(50);
        mockMvc.perform(
            get("/health")
                .accept(APPLICATION_JSON_VALUE)
        ).andExpect(status().is(200))
            .andExpect(jsonPath("$.status", is(200)))
            .andExpect(jsonPath("$.startTime", is("2021-07-06T00:00:00Z")))
            .andExpect(jsonPath("$.upTime", is("PT0.05S")))
            .andExpect(jsonPath("$.message", is("Application ready")));
    }

    @Test
    void statusStopping() throws Exception {
        healthController.onApplicationStoppedEvent(null);

        mockMvc.perform(
            get("/health")
                .accept(APPLICATION_JSON_VALUE)
        ).andExpect(status().is(503))
            .andExpect(jsonPath("$.status", is(503)))
            .andExpect(jsonPath("$.message", is("Application shutdown")));
    }

    @Test
    void prometheusSlow() throws Exception {

        assertThat(healthController.prometheusDownCount).hasValue(0);

        healthController.prometheusController.getDuration().accept(
            Duration.ofSeconds(20));

        mockMvc.perform(
            get("/health")
                .accept(APPLICATION_JSON_VALUE)
        ).andExpect(status().is(200))
            .andExpect(jsonPath("$.status", is(200)))
            .andExpect(jsonPath("$.prometheusCallDuration", is("PT20S")))
            .andExpect(jsonPath("$.prometheusDownCount", is(1)))
            .andExpect(jsonPath("$.message", is("Application ready")))
        ;

        assertThat(healthController.prometheusDownCount).hasValue(1);
    }
}
