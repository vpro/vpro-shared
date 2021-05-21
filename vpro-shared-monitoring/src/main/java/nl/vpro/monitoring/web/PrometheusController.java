package nl.vpro.monitoring.web;

import io.micrometer.prometheus.PrometheusMeterRegistry;
import io.prometheus.client.exporter.common.TextFormat;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.io.Writer;

import javax.servlet.http.HttpServletResponse;

import org.springframework.context.annotation.Lazy;
import org.springframework.web.bind.annotation.*;

@Lazy(false)
@RestController
@RequestMapping(value = "/prometheus", produces = TextFormat.CONTENT_TYPE_004)
@Slf4j
public class PrometheusController {

    private final PrometheusMeterRegistry registry;

    public PrometheusController(PrometheusMeterRegistry registry) {
        this.registry = registry;
    }

    @GetMapping
    public void metrics(final HttpServletResponse response) throws IOException {
        log.debug("Scraping Prometheus metrics");

        response.setStatus(HttpServletResponse.SC_OK);
        response.setContentType(TextFormat.CONTENT_TYPE_004);

        try (Writer writer = response.getWriter()) {
            registry.scrape(writer);
            writer.flush();
        }
    }

}
