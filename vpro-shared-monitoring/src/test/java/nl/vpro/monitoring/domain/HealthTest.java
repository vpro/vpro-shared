package nl.vpro.monitoring.domain;

import tools.jackson.databind.ObjectMapper;

import java.time.Duration;
import java.time.Instant;

import org.junit.jupiter.api.Test;

import static nl.vpro.test.util.jackson3.Jackson3TestUtil.assertThatJson;

class HealthTest {


    @Test
    public void json() {
        Health health = new Health(
            200,
            "ok",
            Instant.EPOCH,
            Duration.ofSeconds(1),
            Duration.ofSeconds(2), 0L);

        ObjectMapper objectMapper = new ObjectMapper();

        assertThatJson(objectMapper, health).isSimilarTo("""
            {
                "status" : 200,
                "message" : "ok",
                "startTime" : "1970-01-01T00:00:00Z",
                "upTime" : "PT1S",
                "prometheusCallDuration" : "PT2S",
                "prometheusDownCount" : 0
             }
            """);
    }
}
