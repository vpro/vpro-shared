package nl.vpro.util;

import java.lang.invoke.MethodHandles;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;

/**
 * @author Michiel Meeuwissen
 * @since 1.8
 */
public class LeaveDefaultsProxyHandler implements InvocationHandler {
    
    private final Object delegate;
    final static Constructor<MethodHandles.Lookup> CONSTRUCTOR;
    static {
        try {
            CONSTRUCTOR = MethodHandles.Lookup.class.getDeclaredConstructor(Class.class, int.class);
        } catch (NoSuchMethodException e) {
            throw new RuntimeException(e);
        }
        if (!CONSTRUCTOR.isAccessible()) {
            CONSTRUCTOR.setAccessible(true);
        }
    }

    public LeaveDefaultsProxyHandler(Object delegate) {
        this.delegate = delegate;
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        if (method.isDefault()) {
            final Class<?> declaringClass = method.getDeclaringClass();
            return CONSTRUCTOR.newInstance(declaringClass, MethodHandles.Lookup.PRIVATE)
                .unreflectSpecial(method, declaringClass)
                .bindTo(proxy)
                .invokeWithArguments(args);
        } else {
            return method.invoke(delegate, args);
        }
    }
}
