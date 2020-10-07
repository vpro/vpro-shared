package nl.vpro.monitoring.domain;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class Health {
    private int status;
    private String message;
}
