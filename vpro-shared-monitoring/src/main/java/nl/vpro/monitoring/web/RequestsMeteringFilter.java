package nl.vpro.monitoring.web;

import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.io.Serial;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
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

    private final List<Matcher> matchers = new ArrayList<>();


    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        String prefixes = filterConfig.getInitParameter("prefixes");
        if (StringUtils.isNotBlank(prefixes)) {
            for (String pref :prefixes.split("[,\\s]+")) {
                String trimmed = pref.trim();
                matchers.add(Matcher.of(pref, s -> s.startsWith(trimmed)));
            }
        }
        matchers.add(Matcher.of("*", Predicates.alwaysTrue()));
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

        long nanoStart = System.nanoTime();
        try {
            chain.doFilter(request, response);
        } finally {
            String contentType = "?";
            {
                if (response.getContentType() != null) {
                    MediaType type = MediaType.parse(response.getContentType());
                    contentType = type.withoutParameters().toString();
                } else {
                    if (200 == response.getStatus()) {
                        log.warn("No content type in {}", path);
                    }
                }
            }
            String[] tags = new String[] {
                "path", pathTag,
                "contentType", contentType,
                "status", String.valueOf(response.getStatus()),
                "method", request.getMethod()
            };
            meterRegistry.counter("servlet_requests", tags).increment();
            meterRegistry.timer("servlet_requests_duration", tags)
                .record(Duration.ofNanos(System.nanoTime() - nanoStart));
        }
    }
}
