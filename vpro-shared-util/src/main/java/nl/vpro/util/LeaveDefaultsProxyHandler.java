package nl.vpro.util;

import java.lang.invoke.MethodHandles;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;

/**
 * Support for java 8 'default' methods. Resteasy won't honour them. Wrapping around this proxy reinstalls them.
 *
 * @author Michiel Meeuwissen
 * @since 1.8
 */
public class LeaveDefaultsProxyHandler implements InvocationHandler {

    private final Object delegate;

    public LeaveDefaultsProxyHandler(Object delegate) {
        this.delegate = delegate;
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        if (method.isDefault()) {
            final Class<?> declaringClass = method.getDeclaringClass();
            return MethodHandles.lookup().in(declaringClass)
                .unreflectSpecial(method, declaringClass)
                .bindTo(proxy)
                .invokeWithArguments(args);

        } else {
            return method.invoke(delegate, args);
        }
    }
}
