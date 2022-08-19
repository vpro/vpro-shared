package nl.vpro.monitoring.web;

import lombok.extern.slf4j.Slf4j;

import java.lang.management.ManagementFactory;
import java.time.*;

import javax.inject.Inject;

import org.checkerframework.checker.nullness.qual.MonotonicNonNull;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.event.*;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.context.WebApplicationContext;

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

    Duration unhealthyThreshold = Duration.ofSeconds(10);

    @Inject
    WebApplicationContext webApplicationContext;

    @Inject
    PrometheusController prometheusController;

    private boolean threadsDumped = false;

    /**
     * This is only triggered if you call ConfigurableApplicationContext#start, which we probably don't.
     */
    @EventListener
    public void onApplicationStartedEvent(ContextStartedEvent startedEvent) {
        markReady(startedEvent);
    }

    @EventListener
    public void onApplicationRefreshedEvent(ContextRefreshedEvent refreshedEventEvent) {
        markReady(refreshedEventEvent);
    }

    protected boolean markReady(ApplicationContextEvent event) {
        if (status != Status.READY) {
            status = Status.READY;
            ready = clock.instant();
            log.info("Status {} at {} ({}) for {}", status, ready, event, webApplicationContext.getApplicationName());
            return true;
        } else {
            log.debug("Ready already ({})", event);
            return false;

        }

    }

    @EventListener
    public void onApplicationStoppedEvent(ContextStoppedEvent stoppedEvent) {
        status = Status.STOPPING;
        log.info("Status {} at {}", status, Instant.now());
    }

    @GetMapping
    public ResponseEntity<Health> health() {
        log.debug("Polling health endpoint");
        boolean prometheusDown = prometheusController != null && prometheusController.getDuration().getWindowValue().durationValue().compareTo(unhealthyThreshold) > 0;
        Status effectiveStatus =  prometheusDown ? Status.UNHEALTHY : this.status;
        if (prometheusDown) {
            if (! threadsDumped) {
                new Thread(null, () -> {
                    log.info("Dumping threads for later analysis");
                    ManagementFactory.getThreadMXBean().dumpAllThreads(true, true);

                }, "Dumping threads").start();
                threadsDumped = true;
            }
        } else {
            threadsDumped = false;
        }

        return ResponseEntity
            .status(effectiveStatus.code)
            .body(
                Health.builder()
                    .status(effectiveStatus.code)
                    .message(effectiveStatus.message)
                    .startTime(ready == null ? null : ready)
                    .upTime(ready == null ? null : Duration.between(ready, clock.instant()))
                    .prometheusCallDuration(prometheusController != null ? prometheusController.getDuration().getWindowValue().durationValue() : null)
                    .build()
            );
    }

    private enum Status {
        STARTING(503, "Application starting"),
        READY(200, "Application ready"),
        STOPPING(503, "Application shutdown"),
        UNHEALTHY(503, "Application is unhealthy")
        ;

        final int code;
        final String message;

        Status(int code, String message) {
            this.code = code;
            this.message = message;
        }
    }
}
