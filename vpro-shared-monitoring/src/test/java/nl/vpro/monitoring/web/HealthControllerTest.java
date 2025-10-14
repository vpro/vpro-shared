package nl.vpro.monitoring.web;

import io.micrometer.prometheusmetrics.PrometheusMeterRegistry;

import java.time.*;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.meeuw.time.TestClock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import nl.vpro.monitoring.config.MonitoringProperties;

import static nl.vpro.test.util.jackson2.Jackson2TestUtil.assertThatJson;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(SpringExtension.class)
@WebAppConfiguration
@ContextConfiguration(classes = Setup.class)
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
        this.healthController.setStatus(HealthController.Status.STARTING);
        this.healthController.prometheusController = new PrometheusController(() -> new PrometheusMeterRegistry(s -> null), new MonitoringProperties());
        this.mockMvc = MockMvcBuilders.webAppContextSetup(this.wac).build();
        this.healthController.markReady(null);
    }


    @Test
    void statusReady() throws Exception {
        healthController.onApplicationStartedEvent(null);
        clock.tick(50);
        String contentAsString = mockMvc.perform(
                get("/health")
                    .accept(APPLICATION_JSON_VALUE)
            ).andExpect(status().is(200))
            .andReturn().getResponse().getContentAsString();

        assertThatJson(contentAsString).isSimilarTo("""
                 {
                   "status" : 200,
                   "message" : "Application ready",
                   "startTime" : "2021-07-06T00:00:00Z",
                   "upTime" : "PT0.05S",
                   "prometheusCallDuration" : "PT0S"
                 }
            """);
    }

    @Test
    void statusStopping() throws Exception {
        healthController.onApplicationStoppedEvent(null);
        clock.tick(500);
        String contentAsString = mockMvc.perform(
            get("/health")
                .accept(APPLICATION_JSON_VALUE)
        ).andExpect(status().is(503)).andReturn().getResponse().getContentAsString();

            assertThatJson(contentAsString).isSimilarTo("""
                {
                         "status" : 503,
                         "message" : "Application shutdown",
                         "startTime" : "2021-07-06T00:00:00Z",
                         "upTime" : "PT0.5S",
                         "prometheusCallDuration" : "PT0S"
                       }
            """);

    }

    @Test
    void prometheusSlow() throws Exception {

        assertThat(healthController.prometheusDownCount).hasValue(0);

        healthController.prometheusController.getDuration().accept(
            Duration.ofSeconds(20));

        String contentAsString = mockMvc.perform(
            get("/health")
                .accept(APPLICATION_JSON_VALUE)
        ).andExpect(status().is(200)).andReturn().getResponse().getContentAsString();

        assertThatJson(contentAsString).isSimilarTo("""
            {
                   "status" : 200,
                   "message" : "Application ready",
                   "startTime" : "2021-07-06T00:00:00Z",
                   "upTime" : "PT0S",
                   "prometheusCallDuration" : "PT20S",
                   "prometheusDownCount" : 1
                 }
            """);


        assertThat(healthController.prometheusDownCount).hasValue(1);
    }
}
