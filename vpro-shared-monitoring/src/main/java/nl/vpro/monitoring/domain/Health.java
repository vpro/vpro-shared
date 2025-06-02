package nl.vpro.monitoring.domain;

import com.fasterxml.jackson.annotation.JsonFormat;

import lombok.Builder;

import java.time.Duration;
import java.time.Instant;


@Builder
public record Health(
    int status,
    String message,
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    Instant startTime,
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    Duration upTime,
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    Duration prometheusCallDuration,
    Long prometheusDownCount) {
}
