package nl.vpro.monitoring.domain;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import nl.vpro.test.util.jackson2.Jackson2TestUtil;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;

import static nl.vpro.test.util.jackson2.Jackson2TestUtil.assertThatJson;
import static org.junit.jupiter.api.Assertions.*;

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
        objectMapper.registerModule(new JavaTimeModule());

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
