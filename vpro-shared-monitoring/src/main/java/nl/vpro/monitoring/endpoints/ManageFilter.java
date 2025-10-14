package nl.vpro.monitoring.endpoints;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

import jakarta.inject.*;
import jakarta.servlet.*;
import jakarta.servlet.http.*;

import org.springframework.http.*;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;

import nl.vpro.monitoring.config.MonitoringProperties;
import nl.vpro.monitoring.domain.Health;
import nl.vpro.monitoring.web.*;

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
    }





    @Override
    protected void doFilter(HttpServletRequest request, HttpServletResponse response, FilterChain chain) throws IOException, ServletException {
        String servletPath = request.getServletPath();
        if (health != null && health.equals(servletPath)) {
            ResponseEntity<Health> entity = healthController.get().health();
            writeResponseEntity(entity, response, MediaType.APPLICATION_JSON);
            return;
        } else if (metrics != null && metrics.equals(servletPath)) {
            prometheusController.get().metrics(request, response);
            return;
        } else if (prometheus != null && prometheus.equals(servletPath)) {
            prometheusController.get().prometheus(request, response);
            return;
        } else if (wellknown && servletPath.startsWith("/.well-known/")) {
            String fileName = servletPath.substring("/.well-known/".length());
            String f = wellKnownController.get().wellKnownFile(fileName, request, response);
            response.setContentType(MediaType.TEXT_PLAIN_VALUE);
            response.getWriter().write(f);
            return;
        }
        chain.doFilter(request, response);

    }

    public void writeResponseEntity(ResponseEntity<?> entity, HttpServletResponse response, MediaType defaultContentType) throws IOException {
        // Set status
        response.setStatus(entity.getStatusCode().value());

        // Set headers
        HttpHeaders headers = entity.getHeaders();
        for (String headerName : headers.keySet()) {
            List<String> values = headers.get(headerName);
            if (values != null) {
                for (String value : values) {
                    response.addHeader(headerName, value);
                }
            }
        }

        MediaType contentType = Optional.ofNullable(headers.getContentType()).orElse(defaultContentType);
        // Write body if present
        Object body = entity.getBody();
        if (body != null) {
            if (MediaType.APPLICATION_JSON.isCompatibleWith(contentType)) {
                response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                objectMapper.writeValue(response.getOutputStream(), entity.getBody());
                return;
            } else {
                response.setContentType(contentType.toString());
                response.getWriter().write(body.toString());
            }
        }
    }
}

