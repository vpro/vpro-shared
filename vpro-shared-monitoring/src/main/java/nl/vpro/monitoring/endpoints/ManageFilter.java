package nl.vpro.monitoring.endpoints;

import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.Getter;
import lombok.Setter;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

import jakarta.inject.*;
import jakarta.servlet.*;
import jakarta.servlet.http.*;

import nl.vpro.monitoring.domain.Health;
import nl.vpro.monitoring.web.*;

import org.springframework.http.*;
import org.springframework.web.context.support.SpringBeanAutowiringSupport;


public class ManageFilter extends HttpFilter {

    @Getter
    @Setter
    private String health = "/manage/health";

    @Getter
    @Setter
    private String metrics = "/manage/metrics";

    @Getter
    @Setter
    private String prometheus = "/manage/prometheus";

    @Inject
    Provider<PrometheusController> prometheusController;
    @Inject
    Provider<HealthController> healthController;
    @Inject
    Provider<WellKnownController> wellKnownController;

    @Inject
    @Named("monitoringObjectMapper")
    ObjectMapper objectMapper;

    @Override
    public void init(FilterConfig filterConfig)  {
        SpringBeanAutowiringSupport
            .processInjectionBasedOnServletContext(this,
                filterConfig.getServletContext());
    }

    @Override
    protected void doFilter(HttpServletRequest request, HttpServletResponse response, FilterChain chain) throws IOException, ServletException {
        String servletPath = request.getServletPath();
        if (health.equals(servletPath)) {
            ResponseEntity<Health> entity = healthController.get().health();
            writeResponseEntity(entity, response, MediaType.APPLICATION_JSON);
            return;
        }
        if (metrics.equals(servletPath)) {
            prometheusController.get().metrics(request, response);
            return;
        }
        if (prometheus.equals(servletPath)) {
            prometheusController.get().prometheus(request, response);
            return;
        }
        if (servletPath.startsWith("/.well-known/")) {
            String f = wellKnownController.get().wellKnownFile(servletPath, request, response);
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
                response.setContentType(headers.getContentType() != null ? headers.getContentType().toString() : "text/plain");
                response.getWriter().write(body.toString());
            }
        }
    }
}

