package nl.vpro.monitoring.web;

import io.micrometer.prometheusmetrics.PrometheusMeterRegistry;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.io.OutputStream;
import java.time.Duration;
import java.util.Optional;

import jakarta.inject.Inject;
import jakarta.inject.Provider;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.meeuw.math.statistics.StatisticalLong;
import org.meeuw.math.windowed.WindowedStatisticalLong;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.http.HttpHeaders;
import org.springframework.web.bind.annotation.*;

import nl.vpro.monitoring.config.*;

import static nl.vpro.logging.Slf4jHelper.debugOrInfo;

@Lazy(false)

@Slf4j
public class PrometheusController {

    public static final String CONTENT_TYPE = "text/plain; version=0.0.4; charset=utf-8";

    @Getter
    private final WindowedStatisticalLong duration = createDuration();

    private final Provider<PrometheusMeterRegistry> registry;

    private final MonitoringProperties properties;




    @Inject
    public PrometheusController(Provider<PrometheusMeterRegistry> registry, @Value("endpointMonitoringProperties") MonitoringProperties properties) {
        this.registry = registry;
        this.properties = properties;
    }

    /**
     * As {@link #prometheus(HttpServletRequest, HttpServletResponse)}. TODO: spring boot actuator does something different.
     * It give s json with all metric names for /metrics.
     * May be we could conform?
     */

    public void metrics(
        HttpServletRequest request,
        HttpServletResponse response

    ) throws IOException {
        prometheus(request, response);
    }


    /**
     * Returns metrics in format fit for prometheus
     */

    public synchronized void prometheus(
        HttpServletRequest request,
        HttpServletResponse response
    ) throws IOException {
        if (authenticate(request, response)) {

            log.debug("Scraping Prometheus metrics");
            String auth = request.getHeader(HttpHeaders.AUTHORIZATION);
            if (auth == null || !auth.startsWith("Basic ")) {
                response.setHeader("WWW-Authenticate", "Basic realm=\"Protected\"");
                response.sendError(HttpServletResponse.SC_UNAUTHORIZED);
                return;
            }

            response.setStatus(HttpServletResponse.SC_OK);
            response.setContentType(CONTENT_TYPE);
            try (
                WindowedStatisticalLong.RunningDuration measure = duration.measure();
                OutputStream writer = response.getOutputStream()) {

                Duration took = scrape(writer);
                debugOrInfo(log, took.compareTo(Duration.ofSeconds(2)) > 0, "Scraping Prometheus metrics took {}", took);
            }
        }
    }
    protected Duration scrape(OutputStream writer) throws IOException {
        try {
            long start = System.nanoTime();
            registry.get().scrape(writer);
            writer.flush();
            return Duration.ofNanos(System.nanoTime() - start);
        } catch (NoSuchBeanDefinitionException noSuchBeanDefinitionException) {
            log.warn("No prometheus registry available");
            writer.write("# No prometheus registry available. Please use %s or %s (or some other way) to register a PrometheusRegistry in your spring application context\n".formatted(EnableMonitoring.class, MeterRegistryConfiguration.class).getBytes());
            writer.flush();
            return Duration.ZERO;
        }
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


    private boolean authenticate(
        HttpServletRequest request,
        HttpServletResponse response
    ) throws IOException {
        return Authentication.basic(request, response, properties);
    }

}
