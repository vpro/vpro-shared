package nl.vpro.monitoring.domain;

import lombok.Builder;

import java.time.Duration;
import java.time.Instant;


@Builder
public record Health(
    int status,
    String message,
    Instant startTime,
    Duration upTime,
    Duration prometheusCallDuration,
    Long prometheusDownCount) {
}
