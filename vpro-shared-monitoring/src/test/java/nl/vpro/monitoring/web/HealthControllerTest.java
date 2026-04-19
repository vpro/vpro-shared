package nl.vpro.monitoring.web;

import io.micrometer.prometheusmetrics.PrometheusMeterRegistry;

import java.io.IOException;
import java.time.*;

import jakarta.servlet.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.meeuw.time.TestClock;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mock.web.*;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.context.web.WebAppConfiguration;

import nl.vpro.monitoring.config.MonitoringProperties;
import nl.vpro.monitoring.endpoints.ManageFilter;

import static nl.vpro.test.util.jackson2.Jackson2TestUtil.assertThatJson;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

@ExtendWith(SpringExtension.class)
@WebAppConfiguration
@ContextConfiguration(classes = TestSetup.class)
class HealthControllerTest {

    @Autowired
    private HealthController healthController;

    @Autowired
    ManageFilter manageFilter;



    private final TestClock clock = new TestClock(ZoneId.of("UT"), Instant.parse("2021-07-06T00:00:00.000Z"));


    @BeforeEach
    public void setup() throws ServletException {
        this.manageFilter.init(new MockFilterConfig());
        this.healthController.clock = clock;
        this.healthController.setStatus(HealthController.Status.STARTING);
        this.healthController.prometheusController = new PrometheusController(() -> new PrometheusMeterRegistry(s -> null), new MonitoringProperties());

        this.healthController.markReady(null);
    }


    @Test
    void statusReady() throws Exception {
        healthController.onApplicationStartedEvent(null);
        clock.tick(50);

        String contentAsString = request(200);

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
        String contentAsString = request(503);

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

        String contentAsString = request(200);


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

    protected String request(int expectedStatus) throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest();

        request.setContentType(APPLICATION_JSON_VALUE);
        request.setServletPath("/manage/health");

        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = Mockito.mock(FilterChain.class);

        manageFilter.doFilter(request, response, chain);
        assertThat(response.getStatus()).isEqualTo(expectedStatus);

        return response.getContentAsString();
    }
}
