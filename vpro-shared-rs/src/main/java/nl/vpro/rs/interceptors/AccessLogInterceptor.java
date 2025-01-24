package nl.vpro.rs.interceptors;

import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.regex.Pattern;

import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.ext.Provider;

import org.slf4j.MDC;
import org.springframework.jmx.export.annotation.ManagedAttribute;
import org.springframework.jmx.export.annotation.ManagedResource;
import org.springframework.stereotype.Component;

import nl.vpro.logging.mdc.MDCConstants;
import nl.vpro.logging.simple.Slf4jSimpleLogger;
import nl.vpro.util.*;


/**
 * TODO: seems a more generic utility. Move it to shared.
 */
@Provider
@Component
@Slf4j
@ManagedResource
public class AccessLogInterceptor implements ContainerRequestFilter {

    private Pattern forUser = Pattern.compile("^(rcrs|npo-sourcing-service.*|functional-tests.*|nep-backend)$");

    private Pattern forContentType = Pattern.compile("^application/(xml|json)");

    private int truncateAfter = 2048;

    private final Map<String, AtomicLong> counters = new ConcurrentHashMap<>();

    private Path filesPath = null;


    @Override
    public void filter(ContainerRequestContext requestContext) throws IOException {

        if ("POST".equals(requestContext.getMethod())) {
            String contentType = requestContext.getHeaderString("content-type");
            String user = MDC.get(MDCConstants.USER_NAME);
            if ((user != null && forUser.matcher(user).matches()) &&
                (contentType != null && forContentType.matcher(contentType).matches())
            ) {
                long count = counters.computeIfAbsent(user, k -> new AtomicLong()).incrementAndGet();
                MDC.put(MDCConstants.USER_COUNT, String.valueOf(count));
                TruncatedObservableInputStream inputStream;
                if (filesPath == null) {
                    inputStream = new LoggingInputStream(Slf4jSimpleLogger.slf4j(log), requestContext.getEntityStream());
                } else {
                    Path file = filesPath.resolve(user + "-" + count + ".log");
                    inputStream = new FileInputStreamTee(Files.newOutputStream(file), requestContext.getEntityStream());
                }
                inputStream.setTruncateAfter(truncateAfter);
                requestContext.setEntityStream(inputStream);
            } else {
                log.trace("Not logging body for {} {}", user, contentType);
            }
        }
    }

    @ManagedAttribute
    public String getForUser() {
        return forUser.pattern();
    }

    @ManagedAttribute
    public void setForUser(String pattern) {
        this.forUser = Pattern.compile(pattern);
    }

    @ManagedAttribute
    public String getForContentType() {
        return forContentType.pattern();
    }

    @ManagedAttribute
    public void setForContentType(String forContentType) {
        this.forContentType = Pattern.compile(forContentType);
    }

    @ManagedAttribute
    public int getTruncateAfter() {
        return truncateAfter;
    }

    @ManagedAttribute
    public void setTruncateAfter(int truncateAfter) {
        this.truncateAfter = truncateAfter;
    }
}
