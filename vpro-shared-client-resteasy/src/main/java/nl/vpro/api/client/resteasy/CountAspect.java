package nl.vpro.api.client.resteasy;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.time.Duration;
import java.util.Map;

import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;

/**
 * Wraps all calls to log client errors, and to register some statistics.
 *
 * @author Michiel Meeuwissen
 * @since 1.57
 */

public class CountAspect<T> implements InvocationHandler {
    private final T proxied;


    private final Map<Method, Counter> counts;
    private final ObjectName name;
    private final Duration countWindow;

    CountAspect(T proxied, Map<Method, Counter> counter, Duration countWindow, ObjectName name) {
        this.proxied = proxied;
        this.counts = counter;
        this.name = name;
        this.countWindow = countWindow;
    }


    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        counts.computeIfAbsent(method, (m) -> new Counter(getObjectName(m), countWindow)).incrementAndGet();
        return method.invoke(proxied, args);
    }

    ObjectName getObjectName(Method m) {
        try {
            return new ObjectName(name.toString() + ",name=" + AbstractApiClient.methodToString(m));
        } catch (MalformedObjectNameException e) {
            throw new RuntimeException(e);
        }
    }


    public static <T> T proxyCounter(Map<Method, Counter> counter, Duration countWindow, ObjectName name, Class<T> restInterface, T service) {
        return (T) Proxy.newProxyInstance(CountAspect.class.getClassLoader(),
            new Class[]{restInterface},
            new CountAspect<T>(service, counter, countWindow, name));
    }

}

