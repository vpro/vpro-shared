package nl.vpro.monitoring.domain;

import lombok.Data;

import java.time.Duration;
import java.time.Instant;

import com.fasterxml.jackson.annotation.JsonProperty;

@Data
@lombok.Builder
public class Health {
    private final int status;
    private final String message;
    private final Instant startTime;


    @JsonProperty
    public Duration getUptime() {
        return Duration.between(startTime, Instant.now());
    }
}
