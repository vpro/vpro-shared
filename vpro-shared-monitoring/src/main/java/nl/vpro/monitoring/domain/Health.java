package nl.vpro.monitoring.domain;

import lombok.Data;

import java.time.Duration;
import java.time.Instant;

@Data
@lombok.Builder
public class Health {
    private final int status;
    private final String message;
    private final Instant startTime;
    private final Duration upTime;
    private final Duration prometheusCallDuration;
    private final Long prometheusDownCount;
}
