package nl.vpro.monitoring.web;

import io.micrometer.prometheusmetrics.PrometheusMeterRegistry;
import io.prometheus.client.exporter.common.TextFormat;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.io.OutputStream;
import java.time.Duration;
import java.util.Base64;
import java.util.Optional;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.checkerframework.checker.nullness.qual.Nullable;
import org.meeuw.math.statistics.StatisticalLong;
import org.meeuw.math.windowed.WindowedStatisticalLong;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.http.HttpHeaders;
import org.springframework.web.bind.annotation.*;

import nl.vpro.monitoring.config.MonitoringProperties;

import static nl.vpro.logging.Slf4jHelper.debugOrInfo;

@Lazy(false)
@RestController
@Slf4j
public class PrometheusController {

    @Getter
    private final WindowedStatisticalLong duration = createDuration();

    @Nullable
    private final PrometheusMeterRegistry registry;


    @Autowired
    private final MonitoringProperties properties;


    @Autowired
    HttpServletResponse response;

    @Autowired
    HttpServletRequest request;

    public PrometheusController(Optional<PrometheusMeterRegistry> registry, MonitoringProperties properties) {
        this.registry = registry.orElse(null);
        this.properties = properties;
    }

    /**
     * As {@link #prometheus())}. TODO: spring boot actuator does something different.
     * It give s json with all metric names for /metrics.
     * May be we could conform?
     */
    @RequestMapping(
        method = RequestMethod.GET,
        value = "/metrics", produces = TextFormat.CONTENT_TYPE_004)

    public void metrics() throws IOException {
        prometheus();
    }


    /**
     * Returns metrics in format fit for prometheus
     */
    @RequestMapping(
        method = RequestMethod.GET,
        value = "/prometheus", produces = TextFormat.CONTENT_TYPE_004)
    public synchronized void prometheus() throws IOException {
        if (authenticate()) {

            log.debug("Scraping Prometheus metrics");
            String auth = request.getHeader(HttpHeaders.AUTHORIZATION);
            if (auth == null || !auth.startsWith("Basic ")) {
                response.setHeader("WWW-Authenticate", "Basic realm=\"Protected\"");
                response.sendError(HttpServletResponse.SC_UNAUTHORIZED);
                return;
            }
            String credentials = new String(Base64.getDecoder().decode(auth.substring(6))); // Remove "Basic "


            response.setStatus(HttpServletResponse.SC_OK);
            response.setContentType(TextFormat.CONTENT_TYPE_004);
            try (
                WindowedStatisticalLong.RunningDuration measure = duration.measure();
                OutputStream writer = response.getOutputStream()) {

                Duration took = scrape(writer);
                debugOrInfo(log, took.compareTo(Duration.ofSeconds(2)) > 0, "Scraping Prometheus metrics took {}", took);
            }
        }
    }
    protected Duration scrape(OutputStream writer) throws IOException {
        long start = System.nanoTime();
        registry.scrape(writer);
        writer.flush();
        return Duration.ofNanos(System.nanoTime() - start);
    }

    public void reset() {
        this.duration.reset();
    }

    private WindowedStatisticalLong createDuration() {
        return WindowedStatisticalLong.builder()
            .mode(StatisticalLong.Mode.DURATION)
            .bucketCount(10)
            .bucketDuration(Duration.ofSeconds(5))
            .build();
    }

    /**
     * We used to do this via spring security, but that's all pretty cumbersome, and some applications (e.g. image frontend does not even need spring security).
     */
    private boolean authenticate() throws IOException {
        String auth = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (auth == null || !auth.startsWith("Basic ")) {
            response.setHeader(HttpHeaders.WWW_AUTHENTICATE, "Basic realm=\"manager\"");
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED);
            return false;
        }
        String credentials = new String(Base64.getDecoder().decode(auth.substring(6))); // Remove "Basic "
        String[] values = credentials.split(":");
        String username = values[0];
        String password = values[1];
        if (properties.getUser().equals(username) && password.equals(properties.getPassword())) {
            return true;
        } else {
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED);
            return false;
        }

    }
}
