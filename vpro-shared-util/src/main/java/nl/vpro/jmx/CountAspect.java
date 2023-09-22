package nl.vpro.jmx;

import lombok.Data;

import java.lang.reflect.*;
import java.net.URI;
import java.time.Duration;
import java.util.Map;

import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;

import nl.vpro.logging.simple.Level;
import nl.vpro.logging.simple.SimpleLogger;

import static nl.vpro.util.TimeUtils.roundToMillis;

/**
 * Wraps all calls to register some statistics.
 *
 * @author Michiel Meeuwissen
 * @since 1.57
 */
public class CountAspect<T> implements InvocationHandler {

    public static final ThreadLocal<Local> currentThreadLocal = ThreadLocal.withInitial(() -> null);

    private final SimpleLogger log;
    private final T proxied;
    private final Map<String, Counter> counts;
    private final ObjectName name;
    private final Duration countWindow;
    private final Duration warnThreshold;
    private final Integer bucketCount;
    private final Level warnLevel;

    CountAspect(T proxied, Map<String, Counter> counter, Duration countWindow, Integer bucketCount, ObjectName name, SimpleLogger log, Duration warnThreshold, Level warnLevel) {
        this.proxied = proxied;
        this.counts = counter;
        this.name = name;
        this.countWindow = countWindow;
        this.bucketCount = bucketCount;
        this.log = log;
        this.warnThreshold = warnThreshold;
        this.warnLevel = warnLevel == null ? Level.WARN : warnLevel;
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        Local local = start(method);
        try {
            return method.invoke(proxied, args);
        } finally {
            local.responseEnd();
            if (local.needsCount()) {
                Duration totalDuration = local.getTotalDuration();
                counts.computeIfAbsent(local.key,
                    (m) -> Counter.builder()
                        .name(getObjectName(m))
                        .countWindow(countWindow)
                        .bucketCount(bucketCount)
                        .build()
                )
                    .eventAndDuration(totalDuration, local.getRequestDuration());

                if (totalDuration.compareTo(warnThreshold) > 0) {
                    String durationReport = (((float) totalDuration.toMillis()) / local.getRequestDuration().toMillis() > 1.5f) ?
                        String.format("%s/%s", roundToMillis(local.getRequestDuration()), roundToMillis(totalDuration)) :
                        roundToMillis(totalDuration).toString();
                    log.log(warnLevel, "Took {}: {} {}",
                        durationReport,
                        local.key,
                        local.requestUri);
                }
            }

            currentThreadLocal.remove();
        }


    }

    ObjectName getObjectName(String m) {
        if (name == null) {
            return null;
        }
        try {
            return new ObjectName(name.toString() + ",name=" + m);
        } catch (MalformedObjectNameException e) {
            throw new RuntimeException(e);
        }
    }


    @SuppressWarnings("unchecked")
    public static <T> T proxyCounter(
        Map<String, Counter> counter,
        Duration countWindow,
        Integer bucketCount,
        ObjectName name, Class<T> restInterface,
        T service,
        SimpleLogger log,
        Duration warnThreshold,
        Level warnLevel
        ) {
        return (T) Proxy.newProxyInstance(CountAspect.class.getClassLoader(),
            new Class[]{restInterface},
            new CountAspect<>(service, counter, countWindow, bucketCount, name, log, warnThreshold, warnLevel));
    }

    private Local start(Method method) {
        Local local = new Local(method);
        currentThreadLocal.set(local);
        return local;
    }
    @Data
    public static class Local {

        final Method method;
        private final long start = System.nanoTime();
        private long requestEnd;
        private long responseEnd;
        private URI requestUri;
        private String key;


        Local(Method method) {
            this.method = method;
            this.key = method.getName();
        }


        public boolean needsCount() {
            return !method.getName().equals("toString");
        }

        public Duration getRequestDuration() {
            return Duration.ofNanos(requestEnd - start);
        }

        public Duration getTotalDuration() {
            return Duration.ofNanos(responseEnd - start);
        }

        public void requestEnd(URI  requestUri, String key) {
            this.requestEnd = System.nanoTime();
            this.requestUri = requestUri;
            this.key = key;
        }
        public void responseEnd() {
            this.responseEnd = System.nanoTime();
        }
    }

}

