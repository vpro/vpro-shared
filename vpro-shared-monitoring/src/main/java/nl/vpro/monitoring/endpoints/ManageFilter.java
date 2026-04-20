package nl.vpro.monitoring.endpoints;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.io.Serial;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import jakarta.inject.*;
import jakarta.servlet.*;
import jakarta.servlet.http.*;

import org.meeuw.functional.ThrowingRunnable;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

import com.fasterxml.jackson.databind.ObjectMapper;

import nl.vpro.monitoring.config.MonitoringProperties;
import nl.vpro.monitoring.domain.Health;
import nl.vpro.monitoring.web.*;
import nl.vpro.util.ThreadPools;

/**
 * There are a lot of spring projects out there that simply capture the entire servlet context. This breaks the idea of adding just a few servlets. Therefore, the 'managements' endpoints are done via this Filter.
 *
 * @since 5.12
 */
@Slf4j
@Component
@Getter
@Setter
public class ManageFilter extends HttpFilter {

    @Serial
    private static final long serialVersionUID = 7490817616301996196L;
    private static final String WELL_KNOWN_PREFIX = "/.well-known/";
    private static final int WELL_KNOWN_PREFIX_LENGTH = WELL_KNOWN_PREFIX.length();
    private static final int FAST_PATH_ASCII_BOUND = 128;

    private final ExecutorService asyncExecutor = ThreadPools.createExecutor(
        Executors::newCachedThreadPool
    );

    @Inject
    Provider<PrometheusController> prometheusController;
    @Inject
    Provider<HealthController> healthController;
    @Inject
    Provider<WellKnownController> wellKnownController;

    @Inject
    @Named("monitoringObjectMapper")
    ObjectMapper objectMapper;

    @Inject
    MonitoringProperties monitoringProperties;

    private String health = "/manage/health";

    private String metrics = "/manage/metrics";

    private String prometheus = "/manage/prometheus";

    private boolean wellknown = true;

    // Fast negative lookup: most requests can bypass this filter via path[1].
    private final boolean[] managedSecondChar = new boolean[FAST_PATH_ASCII_BOUND];

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        super.init(filterConfig);

        health = monitoringProperties.getHealth();
        metrics = monitoringProperties.getMetrics();
        prometheus = monitoringProperties.getPrometheus();
        if (monitoringProperties.getWellknown() == null) {
            wellknown = "".equals(filterConfig.getServletContext().getContextPath());
            if (wellknown) {
                log.debug("Since this runs on /, .well-known is default enabled");
            } else {
                log.debug("Since this does not run on /, .well-known is default disabled");
            }
        } else {
            wellknown = monitoringProperties.getWellknown();
        }
        initFastPathHints();
    }

    private void initFastPathHints() {
        Arrays.fill(managedSecondChar, false);
        addSecondCharCandidate(health);
        addSecondCharCandidate(metrics);
        addSecondCharCandidate(prometheus);
        if (wellknown) {
            addSecondCharCandidate(WELL_KNOWN_PREFIX);
        }
    }

    private void addSecondCharCandidate(String path) {
        if (path != null && path.length() > 1) {
            char c = path.charAt(1);
            if (c < FAST_PATH_ASCII_BOUND) {
                managedSecondChar[c] = true;
            }
        }
    }


    @Override
    protected void doFilter(HttpServletRequest request, HttpServletResponse response, FilterChain chain) throws IOException, ServletException {
        final String servletPath = request.getServletPath();

        // Hot path: quickly skip this filter for clearly unrelated routes.
        if (servletPath == null || servletPath.length() < 2) {
            chain.doFilter(request, response);
            return;
        }
        char secondChar = servletPath.charAt(1);
        if (secondChar < FAST_PATH_ASCII_BOUND && !managedSecondChar[secondChar]) {
            chain.doFilter(request, response);
            return;
        }

        if (health != null && health.equals(servletPath)) {
            ResponseEntity<Health> entity = healthController.get().health();
            writeResponseEntity(entity, response, MediaType.APPLICATION_JSON);
            return;
        } else if (metrics != null && metrics.equals(servletPath)) {
            handlePrometheusAsync(request, response, () -> prometheusController.get().metrics(request, response));
            return;
        } else if (prometheus != null && prometheus.equals(servletPath)) {
            handlePrometheusAsync(request, response, () -> prometheusController.get().prometheus(request, response));
            return;
        } else if (wellknown && servletPath.startsWith(WELL_KNOWN_PREFIX)) {
            String fileName = servletPath.substring(WELL_KNOWN_PREFIX_LENGTH);
            try {
                String f = wellKnownController.get().wellKnownFile(fileName, request, response);
                response.setContentType(MediaType.TEXT_PLAIN_VALUE);
                response.getWriter().write(f);
            } catch (ResponseStatusException e) {
                int status = e.getStatusCode().value();
                if (status >= 500) {
                    log.error("Error serving /.well-known/{}: {}", fileName, e.getReason());
                } else {
                    log.debug("Returning {} for /.well-known/{}: {}", status, fileName, e.getReason());
                }
                response.sendError(status, e.getReason());
            }
            return;
        }
        chain.doFilter(request, response);

    }

    private void handlePrometheusAsync(HttpServletRequest request, HttpServletResponse response, ThrowingRunnable<IOException> task) {
        AsyncContext asyncContext = request.startAsync(request, response);
        asyncExecutor.submit(() -> {
            try {
                task.run();
            } catch (Exception e) {
                log.error("Error during async prometheus handling", e);
                try {
                    response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                } catch (IOException ex) {
                    log.error("Failed to send error response", ex);
                }
            } finally {
                asyncContext.complete();
            }
        });
    }



    public void writeResponseEntity(ResponseEntity<?> entity, HttpServletResponse response, MediaType defaultContentType) throws IOException {
        // Set status
        response.setStatus(entity.getStatusCode().value());

        // Set headers
        HttpHeaders headers = entity.getHeaders();
        for (var header : headers.entrySet()) {
            List<String> values = header.getValue();
            if (values != null) {
                for (String value : values) {
                    response.addHeader(header.getKey(), value);
                }
            }
        }

        MediaType contentType = headers.getContentType();
        if (contentType == null) {
            contentType = defaultContentType;
        }

        // Write body if present
        Object body = entity.getBody();
        if (body != null) {
            if (MediaType.APPLICATION_JSON.isCompatibleWith(contentType)) {
                response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                objectMapper.writeValue(response.getOutputStream(), body);
                return;
            }
            response.setContentType(contentType.toString());
            response.getWriter().write(body.toString());
        }
    }
}
