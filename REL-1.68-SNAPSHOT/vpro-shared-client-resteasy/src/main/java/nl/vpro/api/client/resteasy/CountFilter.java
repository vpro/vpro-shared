package nl.vpro.api.client.resteasy;

import java.io.IOException;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;
import javax.ws.rs.client.ClientRequestContext;
import javax.ws.rs.client.ClientRequestFilter;
import javax.ws.rs.client.ClientResponseContext;
import javax.ws.rs.client.ClientResponseFilter;

import org.slf4j.Logger;

/**
 * @author Michiel Meeuwissen
 * @since 1.65
 */
class CountFilter implements ClientRequestFilter, ClientResponseFilter  {

    private final Map<String, Counter> counts;
    private final ObjectName name;
    private final Duration countWindow;
    private final Integer bucketCount;
    private final long warnThresholdNanos;
    private final Logger log;



    CountFilter(Map<String, Counter> counter, Duration countWindow, Integer bucketCount, Duration warnThreshold, ObjectName name, Logger log) {
        this.counts = counter;
        this.name = name;
        this.countWindow = countWindow;
        this.bucketCount = bucketCount;
        this.warnThresholdNanos = warnThreshold.toNanos();
        this.log = log;
    }

    @Override
    public void filter(ClientRequestContext requestContext) throws IOException {
        requestContext.setProperty("startTime", System.nanoTime());
    }

    @Override
    public void filter(ClientRequestContext requestContext, ClientResponseContext responseContext) throws IOException {
        CountAspect.Local local = CountAspect.currentThreadLocal.get();
        String key = null;
        if (local != null) {
            key = AbstractApiClient.methodToString(local.method);
            if (responseContext.getStatus() != 200) {
                key += "/" + responseContext.getStatus();
            }
            String cached = (String) requestContext.getProperty("cached");
            if (cached != null) {
                key += "/" + cached;
            }
            local.counted = true;
        } else {
            log.warn("No count aspect local found for {}", requestContext.getUri());
        }
        long duration = System.nanoTime() - (long) requestContext.getProperty("startTime");
        if (key != null) {
            counts.computeIfAbsent(key,
                (m) -> new Counter(getObjectName(m), countWindow, bucketCount))
                .eventAndDuration(Duration.ofNanos(duration));
        }

        if (duration > warnThresholdNanos) {
            log.warn("Took {}: {} {}",
                Duration.ofMillis(TimeUnit.MILLISECONDS.convert(duration, TimeUnit.NANOSECONDS)), // round to ms.
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
