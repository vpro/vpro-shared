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
    private WindowedStatisticalLong duration = createDuration();

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
            Duration scrape = scrape(writer);
            duration.accept(scrape);
        }
    }
    protected Duration scrape(Writer writer) throws IOException {
        long start = System.nanoTime();
        registry.scrape(writer);
        writer.flush();
        return Duration.ofNanos(System.nanoTime() - start);
    }

    public void reset() {
        this.duration = createDuration();
    }

    private WindowedStatisticalLong createDuration() {

        return WindowedStatisticalLong.builder()
            .mode(StatisticalLong.Mode.DURATION)
            .bucketCount(10)
            .bucketDuration(Duration.ofSeconds(5))
            .build();
    }
}
