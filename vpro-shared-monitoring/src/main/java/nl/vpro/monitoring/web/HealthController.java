package nl.vpro.monitoring.web;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.event.*;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import nl.vpro.monitoring.domain.Health;

@Lazy(false)
@RestController
@RequestMapping(value = "/health", produces = MediaType.APPLICATION_JSON_VALUE)
public class HealthController {
    private static final Logger LOG = LoggerFactory.getLogger(HealthController.class);

    private Status status = Status.STARTING;

    @EventListener
    public void onApplicationEvent(ContextRefreshedEvent refreshedEvent) {
        status = Status.READY;
    }

    @EventListener
    public void onApplicationEvent(ContextStoppedEvent stoppedEvent) {
        status = Status.STOPPING;
    }

    @GetMapping
    public ResponseEntity<Health> health() {
        LOG.debug("Polling health endpoint");
        return ResponseEntity.status(status.code).body(Health.builder().status(status.code).message(status.message).build());
    }

    private enum Status {
        STARTING(503, "Application starting"),
        READY(200, "Application ready"),
        STOPPING(503, "Application shutdown");

        int code;
        String message;

        Status(int code, String message) {
            this.code = code;
            this.message = message;
        }
    }
}
