package nl.vpro.api.client.resteasy;

import lombok.extern.slf4j.Slf4j;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.time.Duration;
import java.util.Arrays;
import java.util.Map;

import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;

/**
 * Wraps all calls to register some statistics.
 * @author Michiel Meeuwissen
 * @since 1.57
 */
@Slf4j
public class CountAspect<T> implements InvocationHandler {

    private final T proxied;
    private final Map<Method, Counter> counts;
    private final ObjectName name;
    private final Duration countWindow;
    private final long warnThresholdNanos;

    CountAspect(T proxied, Map<Method, Counter> counter, Duration countWindow, Duration warnThreshold, ObjectName name) {
        this.proxied = proxied;
        this.counts = counter;
        this.name = name;
        this.countWindow = countWindow;
        this.warnThresholdNanos = warnThreshold.toNanos();
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        counts.computeIfAbsent(method,
            (m) -> new Counter(getObjectName(m), countWindow)).incrementAndGet();
        long start = System.nanoTime();

        Object o =  method.invoke(proxied, args);

        long duration = System.nanoTime() - start;
        if (duration > warnThresholdNanos) {
            log.warn("Took {}: {} {} {}",
                Duration.ofNanos(duration),
                method, args == null ? "" : Arrays.asList(args));
        }
        return o;
    }

    ObjectName getObjectName(Method m) {
        try {
            return new ObjectName(name.toString() + ",name=" + AbstractApiClient.methodToString(m));
        } catch (MalformedObjectNameException e) {
            throw new RuntimeException(e);
        }
    }


    static <T> T proxyCounter(
        Map<Method, Counter> counter,
        Duration countWindow,
        Duration warnThreshold,
        ObjectName name, Class<T> restInterface, T service) {
        return (T) Proxy.newProxyInstance(CountAspect.class.getClassLoader(),
            new Class[]{restInterface},
            new CountAspect<T>(service, counter, countWindow, warnThreshold, name));
    }

}

