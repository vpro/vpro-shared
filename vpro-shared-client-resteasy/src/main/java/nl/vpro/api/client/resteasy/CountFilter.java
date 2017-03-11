package nl.vpro.api.client.resteasy;

import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.time.Duration;
import java.util.Map;

import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;
import javax.ws.rs.client.ClientRequestContext;
import javax.ws.rs.client.ClientRequestFilter;
import javax.ws.rs.client.ClientResponseContext;
import javax.ws.rs.client.ClientResponseFilter;

/**
 * @author Michiel Meeuwissen
 * @since 1.65
 */
@Slf4j
class CountFilter implements ClientRequestFilter, ClientResponseFilter  {

    private final Map<String, Counter> counts;
    private final ObjectName name;
    private final Duration countWindow;
    private final long warnThresholdNanos;


    CountFilter(Map<String, Counter> counter, Duration countWindow, Duration warnThreshold, ObjectName name) {
        this.counts = counter;
        this.name = name;
        this.countWindow = countWindow;
        this.warnThresholdNanos = warnThreshold.toNanos();
    }

    @Override
    public void filter(ClientRequestContext requestContext) throws IOException {
        requestContext.setProperty("startTime", System.nanoTime());
    }

    @Override
    public void filter(ClientRequestContext requestContext, ClientResponseContext responseContext) throws IOException {
        CountAspect.Local local = CountAspect.currentThreadLocal.get();
        String key = AbstractApiClient.methodToString(local.method);
        if (responseContext.getStatus() != 200) {
            key += "/" + responseContext.getStatus();
        }
        String cached = (String) requestContext.getProperty("cached");
        if (cached != null) {
            key += "/" + cached;
        }
        local.counted = true;
        Counter counter = counts.computeIfAbsent(key,
            (m) -> new Counter(getObjectName(m), countWindow));
        counter.incrementAndGet();
        long duration = System.nanoTime() - (long) requestContext.getProperty("startTime");
        counter.getDurations().accept(duration);

        if (duration > warnThresholdNanos) {
            log.warn("Took {}: {} {}",
                Duration.ofNanos(duration),
                key,
                requestContext.getUri());
        }

    }

    ObjectName getObjectName(String m) {
        try {
            return new ObjectName(name.toString() + ",name=" + m.replaceAll(":", "_"));
        } catch (MalformedObjectNameException e) {
            throw new RuntimeException(e);
        }
    }
}
