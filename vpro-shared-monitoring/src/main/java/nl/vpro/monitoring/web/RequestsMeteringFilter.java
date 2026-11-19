package nl.vpro.monitoring.web;

import io.micrometer.core.instrument.Gauge;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.io.Serial;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Predicate;

import jakarta.servlet.*;
import jakarta.servlet.http.*;

import org.apache.commons.lang3.StringUtils;
import org.meeuw.functional.Predicates;

import com.google.common.net.MediaType;

import static nl.vpro.monitoring.config.MeterRegistryConfiguration.meterRegistry;


/**
 * Just put something like this in {@code web.xml} and meter all requests.
 * <pre>
 * {@code
 *   <filter>
 *     <filter-name>RequestsMeteringFilter</filter-name>
 *     <filter-class>nl.vpro.monitoring.web.RequestsMeteringFilter</filter-class>
 *     <async-supported>true</async-supported>
 *     <init-param>
 *       <param-name>prefixes</param-name>
 *       <param-value>
 *           /manage
 *           /import
 *       </param-value>
 *     </init-param>
 *   </filter>
 *   <filter-mapping>
 *     <filter-name>RequestsMeteringFilter</filter-name>
 *     <url-pattern>/*</url-pattern>
 *   </filter-mapping>
 *   }
 *  </pre>
 *
 *  The {@code prefixes} parameter defines a lists of recognized 'prefixes'. The path of the request is matched to those, and the first match will result a tag with this prefix value. (e.g. {@code path=/manage}). If none matched then the used tag will be {@code path=*}.
 */
@Slf4j
public class RequestsMeteringFilter extends HttpFilter {

    @Serial
    private static final long serialVersionUID = 9121689985264979972L;

    public static abstract class Matcher implements Predicate<String> {

        private final String toString;

        protected Matcher(String toString) {
            this.toString = toString;
        }

        @Override
        public String toString() {
            return toString;

        }
        public static Matcher of(String string, Predicate<String> predicate) {
            return new Matcher(string) {
                @Override
                public boolean test(String s) {
                    return predicate.test(s);
                }
            };
        }
    }

    static final String NO_ERROR = "none";
    static final String TIMEOUT = "timeout";

    private final List<Matcher> matchers = new ArrayList<>();

    private final AtomicInteger activeRequests = new AtomicInteger();

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        String prefixes = filterConfig.getInitParameter("prefixes");
        if (StringUtils.isNotBlank(prefixes)) {
            for (String pref : prefixes.trim().split("[,\\s]+")) {
                if (pref.isEmpty()) {
                    continue;
                }
                matchers.add(Matcher.of(pref, s -> s.startsWith(pref)));
            }
        }
        matchers.add(Matcher.of("*", Predicates.alwaysTrue()));

        Gauge.builder("servlet_requests_active", activeRequests, AtomicInteger::get)
            .description("Number of requests currently being served")
            .register(meterRegistry);
    }

    @Override
    public void doFilter(HttpServletRequest request, HttpServletResponse response, FilterChain chain) throws IOException, ServletException {
        final String path =
            request.getRequestURI().substring(request.getContextPath().length());

        String pathTag = null;
        for (Matcher pref : matchers) {
            if (pref.test(path)) {
                pathTag = pref.toString;
                break;
            }
        }
        final String finalPathTag = pathTag;

        final long nanoStart = System.nanoTime();
        activeRequests.incrementAndGet();
        boolean async = false;
        String error = NO_ERROR;
        try {
            chain.doFilter(request, response);
            // chain.doFilter returning does not mean the request is done: if something downstream
            // called request.startAsync(), the real completion happens later, on another thread.
            // Recording now (in the finally block below) would decrement activeRequests and record
            // the duration far too early, so defer to an AsyncListener instead.
            async = request.isAsyncStarted();
            if (async) {
                final AtomicBoolean recorded = new AtomicBoolean(false);
                final AtomicReference<String> asyncError = new AtomicReference<>(NO_ERROR);
                request.getAsyncContext().addListener(new AsyncListener() {
                    @Override
                    public void onComplete(AsyncEvent event) {
                        // onComplete is always called eventually, also after a timeout or an error
                        // (then typically after an error dispatch, so the status is final by now)
                        if (recorded.compareAndSet(false, true)) {
                            recordCompletion(request, response, path, finalPathTag, nanoStart, true, asyncError.get());
                        }
                    }

                    @Override
                    public void onTimeout(AsyncEvent event) {
                        // not recording, onComplete will follow
                        log.debug("Async request {} timed out", path);
                        asyncError.compareAndSet(NO_ERROR, TIMEOUT);
                    }

                    @Override
                    public void onError(AsyncEvent event) {
                        // not recording, onComplete will follow
                        log.debug("Async request {} errored: {}", path, String.valueOf(event.getThrowable()));
                        asyncError.compareAndSet(NO_ERROR, errorTag(event.getThrowable()));
                    }

                    @Override
                    public void onStartAsync(AsyncEvent event) {
                        // not re-registering: we only meter the original dispatch's lifecycle
                    }
                });
            }
        } catch (Throwable t) {
            // the container will typically turn this into a 500, but the status isn't set yet
            error = errorTag(t);
            throw t;
        } finally {
            if (!async) {
                recordCompletion(request, response, path, finalPathTag, nanoStart, false, error);
            }
        }
    }

    static String errorTag(Throwable t) {
        return t == null ? "unknown" : t.getClass().getSimpleName();
    }

    private void recordCompletion(HttpServletRequest request, HttpServletResponse response, String path, String pathTag, long nanoStart, boolean async, String error) {

        activeRequests.decrementAndGet();
        // Async requests are only recorded on completion, so the effective content type is known here too.
        String contentType = "?";
        if (response.getContentType() != null) {
            MediaType type = MediaType.parse(response.getContentType());
            contentType = type.withoutParameters().toString();
        } else if (200 == response.getStatus()) {
            log.warn("No content type in {}", path);
        }
        String[] tags = new String[] {
            "path", pathTag,
            "contentType", contentType,
            "status", String.valueOf(response.getStatus()),
            "method", request.getMethod(),
            "async", String.valueOf(async),
            "error", error
        };
        meterRegistry.counter("servlet_requests", tags).increment();
        meterRegistry.timer("servlet_requests_duration", tags)
            .record(Duration.ofNanos(System.nanoTime() - nanoStart));
    }
}
