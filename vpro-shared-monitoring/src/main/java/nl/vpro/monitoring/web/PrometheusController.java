package nl.vpro.monitoring.web;

import io.micrometer.prometheus.PrometheusMeterRegistry;
import io.prometheus.client.exporter.common.TextFormat;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.io.Writer;
import java.time.Duration;

import javax.servlet.http.HttpServletResponse;

import org.meeuw.math.statistics.StatisticalLong;
import org.meeuw.math.windowed.WindowedStatisticalLong;
import org.springframework.context.annotation.Lazy;
import org.springframework.web.bind.annotation.*;

@Lazy(false)
@RestController
@RequestMapping(value = "/prometheus", produces = TextFormat.CONTENT_TYPE_004)
@Slf4j
public class PrometheusController {

    @Getter
    private final  WindowedStatisticalLong duration = WindowedStatisticalLong.builder()
        .mode(StatisticalLong.Mode.DURATION)
        .bucketCount(10)
        .bucketDuration(Duration.ofMinutes(5))
        .build();

    private final PrometheusMeterRegistry registry;

    public PrometheusController(PrometheusMeterRegistry registry) {
        this.registry = registry;
    }

    @GetMapping
    public void metrics(final HttpServletResponse response) throws IOException {
        log.debug("Scraping Prometheus metrics");

        response.setStatus(HttpServletResponse.SC_OK);
        response.setContentType(TextFormat.CONTENT_TYPE_004);
        long start = System.nanoTime();
        try (Writer writer = response.getWriter()) {
            registry.scrape(writer);
            writer.flush();
        } finally {
            duration.accept(Duration.ofNanos(System.nanoTime() - start));
        }

    }

}
