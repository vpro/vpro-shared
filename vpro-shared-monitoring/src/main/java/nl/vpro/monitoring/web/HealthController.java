package nl.vpro.monitoring.web;

import io.micrometer.common.util.StringUtils;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.lang.management.ThreadInfo;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Predicate;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.checkerframework.checker.nullness.qual.MonotonicNonNull;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.event.*;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.context.WebApplicationContext;

import nl.vpro.monitoring.config.MonitoringProperties;
import nl.vpro.monitoring.domain.Health;
import nl.vpro.util.TimeUtils;

import static org.slf4j.event.Level.DEBUG;
import static org.slf4j.event.Level.INFO;

@Lazy(false)
@Slf4j
public class HealthController {

    @Getter
    @Setter
    private Status status = Status.STARTING;

    @MonotonicNonNull
    private Instant ready  = null;

    Clock clock = Clock.systemDefaultZone();


    @Inject
    WebApplicationContext webApplicationContext;

    @Inject
    PrometheusController prometheusController;


    @Inject
    @Named("endpointMonitoringProperties")
    MonitoringProperties monitoringProperties;

    private Instant threadsDumped = null;

    protected AtomicLong prometheusDownCount = new AtomicLong(0);

    @Inject
    HttpServletRequest request;
    @Inject
    HttpServletResponse response;

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
        log.info("Status {} at {}", status, clock.instant());
    }

    @GetMapping
    public ResponseEntity<Health> health() throws IOException {
        log.debug("Polling health endpoint");
        if (monitoringProperties.isHealthPermitAll() || Authentication.basic(request, response, monitoringProperties)) {
            final Duration unhealth = TimeUtils.parseDurationOrThrow(monitoringProperties.getUnhealthyThreshold());
            final Duration minThreadDumpInterval = TimeUtils.parseDurationOrThrow(monitoringProperties.getMinThreadDumpInterval());
            final Predicate<Duration> unhealthy = d -> d.compareTo(unhealth) > 0;

            Duration prometheusDuration = prometheusController == null ? Duration.ZERO : prometheusController.getDuration().getWindowValue().optionalDurationValue().orElse(Duration.ZERO);
            boolean prometheusDown = unhealthy.test(prometheusDuration);

            if (prometheusDown) {
                prometheusDownCount.incrementAndGet();

                if (StringUtils.isBlank(monitoringProperties.getDataDir())) {
                    log.warn("No data dir, can't dump threads");
                } else {
                    synchronized (HealthController.class) {
                        final Instant now = clock.instant();
                        if (threadsDumped == null || threadsDumped.isBefore(now.minus(minThreadDumpInterval))) {
                            final Path threadFile = Path.of(monitoringProperties.getDataDir(), "threads-" + now.toString().replace(':', '-') + ".txt");
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
                            threadsDumped = now;
                        } else {
                            log.info("Skipping thread dump, because it was done recently");
                        }
                    }
                }
            } else {
                if (prometheusDownCount.get() > 0) {
                    log.info("Prometheus seems up again");
                    prometheusDownCount.set(0);
                }
            }
            Status effectiveStatus = prometheusDownCount.get() > monitoringProperties.getUnhealthyCount() ? Status.UNHEALTHY : this.status;
            log.atLevel(effectiveStatus != Status.READY ? INFO : DEBUG).log("Effective status {} (prometheus down: {})", effectiveStatus, prometheusDown);

            return ResponseEntity
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
        } else {
            return ResponseEntity.status(401).build();
        }

    }

    public enum Status {
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
