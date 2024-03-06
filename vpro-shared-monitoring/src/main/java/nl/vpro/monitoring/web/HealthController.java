package nl.vpro.monitoring.web;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.output.NullWriter;
import org.checkerframework.checker.nullness.qual.MonotonicNonNull;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.event.*;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.context.WebApplicationContext;

import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.lang.management.ThreadInfo;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.*;
import java.util.concurrent.atomic.AtomicLong;

import javax.inject.Inject;


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

    @Value("${data.dir}")
    String dataDir;

    private boolean threadsDumped = false;

    protected AtomicLong prometheusDownCount = new AtomicLong(0);

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

        Duration prometheusDuration =  prometheusController == null ? Duration.ZERO : prometheusController.getDuration().getWindowValue().optionalDurationValue().orElse(Duration.ZERO);
        boolean prometheusDown = prometheusDown(prometheusDuration);

        if (prometheusDown) {
            prometheusDownCount.incrementAndGet();
            try {
                Duration secondPrometheusDuration = prometheusController.scrape(NullWriter.INSTANCE);
                prometheusDown = prometheusDown(secondPrometheusDuration);
                if (prometheusDown) {
                    log.warn("Prometheus call took {} > {}. Considering DOWN", secondPrometheusDuration, unhealthyThreshold);
                } else {
                    log.debug("Prometheus is up again");
                }
            } catch (IOException ioa) {
                log.warn(ioa.getMessage());
            }
            if (prometheusDown) {
                synchronized (HealthController.class) {
                    if (!threadsDumped) {
                        final Path threadFile = Path.of(dataDir, "threads-" + Instant.now().toString().replace(':', '-') + ".txt");
                        new Thread(null, () -> {
                            log.info("Dumping threads to {} for later analysis", threadFile);
                            StringBuilder builder = new StringBuilder();

                            for (ThreadInfo threadInfo : ManagementFactory.getThreadMXBean().dumpAllThreads(true, true)) {
                                builder.append(threadInfo.toString());
                                builder.append('\n');
                            }
                            try {
                                Files.writeString(threadFile, builder.toString());
                            } catch (IOException e) {
                                log.warn(e.getMessage(), e);
                            }

                        }, "Dumping threads").start();
                        threadsDumped = true;
                    }
                }
            } else {
                threadsDumped = false;
            }

        } else {
            prometheusDownCount.set(0);
            threadsDumped = false;
        }

        Status effectiveStatus =  prometheusDown ? Status.UNHEALTHY : this.status;
        Health health = Health.builder()
            .status(effectiveStatus.code)
            .message(effectiveStatus.message)
            .startTime(ready == null ? null : ready)
            .upTime(ready == null ? null : Duration.between(ready, clock.instant()))
            .build();

        ResponseEntity<Health> body = ResponseEntity
            .status(effectiveStatus.code)
            .body(
                Health.builder()
                    .status(effectiveStatus.code)
                    .message(effectiveStatus.message)
                    .startTime(ready == null ? null : ready)
                    .upTime(ready == null ? null : Duration.between(ready, clock.instant()))
                    .prometheusCallDuration(prometheusDuration)
                    .prometheusDownCount(prometheusDownCount.get() == 0 ? null : prometheusDownCount.get())
                    .build()
            );

        return body;
    }

    protected boolean prometheusDown(Duration d) {
        return d.compareTo(unhealthyThreshold) > 0;
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
