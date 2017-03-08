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
public class CountAspect<T> implements InvocationHandler {

    static ThreadLocal<Local> currentThreadLocal = ThreadLocal.withInitial(() -> null);

    private final T proxied;
    private final Map<String, Counter> counts;
    private final ObjectName name;
    private final Duration countWindow;

    CountAspect(T proxied, Map<String, Counter> counter, Duration countWindow, ObjectName name) {
        this.proxied = proxied;
        this.counts = counter;
        this.name = name;
        this.countWindow = countWindow;
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        Local local = new Local(method);
        currentThreadLocal.set(local);
        try {
            Object o = method.invoke(proxied, args);

            if (! local.counted) {
                counts.computeIfAbsent(AbstractApiClient.methodToString(method),
                    (m) -> new Counter(getObjectName(m), countWindow))
                    .incrementAndGet();
            }
            long start = System.nanoTime();

            return o;
        } finally {
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
        ObjectName name, Class<T> restInterface, T service) {
        return (T) Proxy.newProxyInstance(CountAspect.class.getClassLoader(),
            new Class[]{restInterface},
            new CountAspect<T>(service, counter, countWindow, name));
    }

    static class Local {
        final Method method;
        boolean counted = false;

        Local(Method method) {
            this.method = method;
        }
    }

}

