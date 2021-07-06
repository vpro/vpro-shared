package nl.vpro.monitoring.domain;

import lombok.Data;

@Data
@lombok.Builder
public class Health {
    private final int status;
    private final String message;
    private final String startTime;
    private final String upTime;
}
