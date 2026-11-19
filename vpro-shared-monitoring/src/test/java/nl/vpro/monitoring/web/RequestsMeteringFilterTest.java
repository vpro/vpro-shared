package nl.vpro.monitoring.web;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Timer;
import io.micrometer.prometheusmetrics.PrometheusConfig;
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry;

import java.io.IOException;

import jakarta.servlet.*;

import org.junit.jupiter.api.*;
import org.springframework.mock.web.*;

import nl.vpro.monitoring.config.MeterRegistryConfiguration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RequestsMeteringFilterTest {

    private PrometheusMeterRegistry previous;
    private PrometheusMeterRegistry registry;
    private RequestsMeteringFilter filter;

    @BeforeEach
    void setup() throws ServletException {
        previous = MeterRegistryConfiguration.meterRegistry;
        registry = new PrometheusMeterRegistry(PrometheusConfig.DEFAULT);
        MeterRegistryConfiguration.meterRegistry = registry;

        MockFilterConfig config = new MockFilterConfig();
        // formatted like it would be in web.xml, so with leading/trailing whitespace
        config.addInitParameter("prefixes", "\n    /manage\n    /import\n  ");
        filter = new RequestsMeteringFilter();
        filter.init(config);
    }

    @AfterEach
    void restore() {
        MeterRegistryConfiguration.meterRegistry = previous;
        registry.close();
    }

    @Test
    void matchingPrefix() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/manage/health");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, (req, resp) -> {
            assertThat(activeRequests()).isEqualTo(1);
            resp.setContentType("application/json; charset=utf-8");
        });

        assertThat(counter("/manage", "application/json", "200", "GET", false).count()).isEqualTo(1);
        assertThat(timer("/manage", "application/json", "200", "GET", false).count()).isEqualTo(1);
        assertThat(activeRequests()).isZero();
    }

    @Test
    void contextPathIsStripped() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/ctx/import/foo");
        request.setContextPath("/ctx");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, (req, resp) -> {
            resp.setContentType("text/plain");
            ((MockHttpServletResponse) resp).setStatus(201);
        });

        assertThat(counter("/import", "text/plain", "201", "POST", false).count()).isEqualTo(1);
    }

    @Test
    void noMatchingPrefix() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/other");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, (req, resp) -> {
            ((MockHttpServletResponse) resp).setStatus(404);
        });

        assertThat(counter("*", "?", "404", "GET", false).count()).isEqualTo(1);
        assertThat(activeRequests()).isZero();
    }

    @Test
    void exceptionInChainIsStillRecorded() {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/manage/fail");
        MockHttpServletResponse response = new MockHttpServletResponse();

        assertThatThrownBy(() ->
            filter.doFilter(request, response, (req, resp) -> {
                throw new IOException("boom");
            })
        ).isInstanceOf(IOException.class);

        assertThat(counter("/manage", "?", "200", "GET", false, "IOException").count()).isEqualTo(1);
        assertThat(activeRequests()).isZero();
    }

    @Test
    void asyncRequestIsRecordedOnCompletion() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/import/async");
        request.setAsyncSupported(true);
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, (req, resp) -> req.startAsync());

        // chain returned, but the request is still in progress
        assertThat(activeRequests()).isEqualTo(1);
        assertThat(registry.find("servlet_requests").counter()).isNull();

        // later, on 'another thread', the async processing finishes
        response.setContentType("application/xml");
        request.getAsyncContext().complete();

        assertThat(activeRequests()).isZero();
        assertThat(counter("/import", "application/xml", "200", "GET", true).count()).isEqualTo(1);
        assertThat(timer("/import", "application/xml", "200", "GET", true).count()).isEqualTo(1);
    }

    @Test
    void asyncTimeoutIsRecordedOnceOnCompletion() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/import/slow");
        request.setAsyncSupported(true);
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, (req, resp) -> req.startAsync());
        MockAsyncContext asyncContext = (MockAsyncContext) request.getAsyncContext();

        // what the container does: onTimeout, error dispatch (status 500), then onComplete
        for (AsyncListener listener : asyncContext.getListeners()) {
            listener.onTimeout(new AsyncEvent(asyncContext));
        }
        assertThat(activeRequests()).isEqualTo(1);
        assertThat(registry.find("servlet_requests").counter()).isNull();

        response.setStatus(500);
        asyncContext.complete();

        assertThat(activeRequests()).isZero();
        assertThat(registry.get("servlet_requests").counters()).hasSize(1);
        assertThat(counter("/import", "?", "500", "GET", true, "timeout").count()).isEqualTo(1);
    }

    @Test
    void asyncErrorIsRecordedOnceOnCompletion() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/manage/broken");
        request.setAsyncSupported(true);
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, (req, resp) -> req.startAsync());
        MockAsyncContext asyncContext = (MockAsyncContext) request.getAsyncContext();

        for (AsyncListener listener : asyncContext.getListeners()) {
            listener.onError(new AsyncEvent(asyncContext, new IOException("broken pipe")));
        }
        assertThat(activeRequests()).isEqualTo(1);
        assertThat(registry.find("servlet_requests").counter()).isNull();

        response.setStatus(500);
        asyncContext.complete();

        assertThat(activeRequests()).isZero();
        assertThat(registry.get("servlet_requests").counters()).hasSize(1);
        assertThat(counter("/manage", "?", "500", "GET", true, "IOException").count()).isEqualTo(1);
    }

    @Test
    void asyncCompleteTwiceIsRecordedOnce() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/manage/twice");
        request.setAsyncSupported(true);
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, (req, resp) -> req.startAsync());
        MockAsyncContext asyncContext = (MockAsyncContext) request.getAsyncContext();

        asyncContext.complete();
        for (AsyncListener listener : asyncContext.getListeners()) {
            listener.onComplete(new AsyncEvent(asyncContext));
        }

        assertThat(activeRequests()).isZero();
        assertThat(counter("/manage", "?", "200", "GET", true).count()).isEqualTo(1);
    }

    @Test
    void multipleConcurrentRequests() throws Exception {
        MockHttpServletRequest request1 = new MockHttpServletRequest("GET", "/manage/1");
        request1.setAsyncSupported(true);
        MockHttpServletRequest request2 = new MockHttpServletRequest("GET", "/manage/2");
        request2.setAsyncSupported(true);

        filter.doFilter(request1, new MockHttpServletResponse(), (req, resp) -> req.startAsync());
        filter.doFilter(request2, new MockHttpServletResponse(), (req, resp) -> req.startAsync());
        assertThat(activeRequests()).isEqualTo(2);

        request1.getAsyncContext().complete();
        assertThat(activeRequests()).isEqualTo(1);

        request2.getAsyncContext().complete();
        assertThat(activeRequests()).isZero();
    }

    private double activeRequests() {
        return registry.get("servlet_requests_active").gauge().value();
    }

    private Counter counter(String path, String contentType, String status, String method, boolean async) {
        return counter(path, contentType, status, method, async, "none");
    }

    private Counter counter(String path, String contentType, String status, String method, boolean async, String error) {
        return registry.get("servlet_requests")
            .tags("path", path, "contentType", contentType, "status", status, "method", method, "async", String.valueOf(async), "error", error)
            .counter();
    }

    private Timer timer(String path, String contentType, String status, String method, boolean async) {
        return registry.get("servlet_requests_duration")
            .tags("path", path, "contentType", contentType, "status", status, "method", method, "async", String.valueOf(async), "error", "none")
            .timer();
    }
}
