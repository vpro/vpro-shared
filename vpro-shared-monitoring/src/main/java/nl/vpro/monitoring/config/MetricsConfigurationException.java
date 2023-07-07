package nl.vpro.monitoring.config;

import java.io.Serial;

public class MetricsConfigurationException extends RuntimeException{
    @Serial
    private static final long serialVersionUID = 5558371948286791423L;

    public MetricsConfigurationException(String message) {
        super(message);
    }
}
