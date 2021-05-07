package nl.vpro.monitoring.web;

import io.micrometer.prometheus.PrometheusMeterRegistry;
import io.prometheus.client.exporter.common.TextFormat;

import java.io.IOException;
import java.io.Writer;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Lazy;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Lazy(false)
@RestController
@RequestMapping(value = "/prometheus", produces = TextFormat.CONTENT_TYPE_004)
public class PrometheusController {
    private static final Logger LOG = LoggerFactory.getLogger(PrometheusController.class);

    private final PrometheusMeterRegistry registry;

    public PrometheusController(PrometheusMeterRegistry registry) {
        this.registry = registry;
    }

    @GetMapping
    public void metrics(final HttpServletResponse response) throws IOException {
        LOG.debug("Scraping Prometheus metrics");

        response.setStatus(HttpServletResponse.SC_OK);
        response.setContentType(TextFormat.CONTENT_TYPE_004);

        try (Writer writer = response.getWriter()) {
            registry.scrape(writer);
            writer.flush();
        }
    }

}
