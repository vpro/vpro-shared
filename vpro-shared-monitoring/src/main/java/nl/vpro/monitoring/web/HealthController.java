package nl.vpro.monitoring.web;

import lombok.extern.slf4j.Slf4j;

import java.time.*;

import org.checkerframework.checker.nullness.qual.MonotonicNonNull;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.event.*;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import nl.vpro.monitoring.domain.Health;

@Lazy(false)
@RestController
@RequestMapping(value = "/health", produces = MediaType.APPLICATION_JSON_VALUE)
@Slf4j
public class HealthController {

    private Status status = Status.STARTING;

    @MonotonicNonNull
    private Instant ready  = null;

    Clock clock = Clock.systemDefaultZone();

    /**
     * This is only triggered if you call ConfigurableApplicationContext#start, which we probably don't.
     */
    @EventListener
    public void onApplicationStartedEvent(ContextStartedEvent startedEvent) {
        markReady();
        log.info("Status {} {} at {}", startedEvent, status, ready);
    }

    @EventListener
    public void onApplicationRefreshedEvent(ContextRefreshedEvent refreshedEventEvent) {
        if (status != Status.READY) {
            markReady();
            log.info("Status {} at {}", status, ready);
        } else {
            log.debug("Ready already ({})", refreshedEventEvent);
        }
    }

    protected void markReady() {
        status = Status.READY;
        ready = clock.instant();
    }

    @EventListener
    public void onApplicationStoppedEvent(ContextStoppedEvent stoppedEvent) {
        status = Status.STOPPING;
        log.info("Status {} at {}", status, Instant.now());
    }

    @GetMapping
    public ResponseEntity<Health> health() {
        log.debug("Polling health endpoint");
        return ResponseEntity
            .status(status.code)
            .body(
                Health.builder()
                    .status(status.code)
                    .message(status.message)
                    .startTime(ready == null ? null : ready.toString())
                    .upTime(ready == null ? null : Duration.between(ready, clock.instant()).toString())
                    .build()
            );
    }

    private enum Status {
        STARTING(503, "Application starting"),
        READY(200, "Application ready"),
        STOPPING(503, "Application shutdown");

        final int code;
        final String message;

        Status(int code, String message) {
            this.code = code;
            this.message = message;
        }
    }
}
