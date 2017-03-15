package nl.vpro.api.client.resteasy;

import lombok.extern.slf4j.Slf4j;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.time.Duration;
import java.util.Map;

import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;

/**
 * Wraps all calls to register some statistics.
 * @author Michiel Meeuwissen
 * @since 1.57
 */
@Slf4j
class CountAspect<T> implements InvocationHandler {

    static ThreadLocal<Local> currentThreadLocal = ThreadLocal.withInitial(() -> null);

    private final T proxied;
    private final Map<String, Counter> counts;
    private final ObjectName name;
    private final Duration countWindow;
    private final Integer bucketCount;

    CountAspect(T proxied, Map<String, Counter> counter, Duration countWindow, Integer bucketCount, ObjectName name) {
        this.proxied = proxied;
        this.counts = counter;
        this.name = name;
        this.countWindow = countWindow;
        this.bucketCount = bucketCount;
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        Local local = new Local(method);
        currentThreadLocal.set(local);
        long start = System.nanoTime();
        try {
            Object o = method.invoke(proxied, args);


            return o;
        } finally {
            if (!local.counted) {
                // Not counted by CountFilter? Count ourselves
                counts.computeIfAbsent(AbstractApiClient.methodToString(method),
                    (m) -> Counter.builder()
                        .name(getObjectName(m))
                        .countWindow(countWindow)
                        .bucketCount(bucketCount)
                        .build()
                )
                    .eventAndDuration(Duration.ofNanos(System.nanoTime() - start));
            }
            currentThreadLocal.remove();
        }


    }

    ObjectName getObjectName(String m) {
        try {
            return new ObjectName(name.toString() + ",name=" + m);
        } catch (MalformedObjectNameException e) {
            throw new RuntimeException(e);
        }
    }


    static <T> T proxyCounter(
        Map<String, Counter> counter,
        Duration countWindow,
        Integer bucketCount,
        ObjectName name, Class<T> restInterface, T service) {
        return (T) Proxy.newProxyInstance(CountAspect.class.getClassLoader(),
            new Class[]{restInterface},
            new CountAspect<T>(service, counter, countWindow, bucketCount, name));
    }

    static class Local {
        final Method method;
        boolean counted = false;

        Local(Method method) {
            this.method = method;
        }
    }

}

