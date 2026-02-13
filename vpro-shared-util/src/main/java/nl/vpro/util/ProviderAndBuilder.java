package nl.vpro.util;

import lombok.extern.slf4j.Slf4j;

import java.lang.reflect.*;
import java.time.Duration;
import java.util.Optional;

import jakarta.inject.Provider;


/**
 * If you have a provider implementation based on a builder, you can fill the fields with the provider to the builder using
 * reflection.
 * Providers can come in useful with IOC-frameworks like guice. The idea is to put {@link jakarta.inject.Named} annotation on the fields of the Provider.
 *
 * @author Michiel MeeuwissenLocaleParamConverter.java
 * @since 1.69
 */
@Slf4j
public class ProviderAndBuilder {


    public static <T, S> S fill(Provider<T> provider, S builder) throws InvocationTargetException, IllegalAccessException, ClassNotFoundException {
        Class<?> providerClass = provider.getClass();
        Class<?> builderClass = builder.getClass();
        for (Field providerField : providerClass.getDeclaredFields()) {
            providerField.setAccessible(true);
            Class<?> providerType = providerField.getType();
            if (providerType.equals(builderClass)) {
                continue;
            }
            Object providerValue = providerField.get(provider);
            if (Optional.class.isAssignableFrom(providerType)) {
                providerType = Class.forName(((ParameterizedType) providerField.getGenericType()).getActualTypeArguments()[0].getTypeName());
                providerValue = providerValue == null ? null : ((Optional) providerValue).orElse(null);
            }
            Method builderMethod = getBuilderMethod(builderClass, providerField.getName(), providerType);
            if (builderMethod != null) {
                try {
                    builderMethod.invoke(builder, convert(providerValue, builderMethod.getParameterTypes()[0]));
                } catch (IllegalArgumentException ia) {
                    throw new IllegalArgumentException(builderMethod + " (" + providerValue + "):"  + ia.getMessage(), ia);
                }
            } else {
                log.info("Ignored {}", providerField);
            }

        }
        return builder;
    }

    protected static Object convert(Object o, Class<?> dest) {
        if (o == null) {
            return null;
        }
        // I couldn't get DurationConvertor working for providers in magnolia. For now let the provider use String's. It doesn't really matter.
        if (CharSequence.class.isInstance(o) && dest.isAssignableFrom(Duration.class)) {
            try {
                return TimeUtils.parseDuration((CharSequence) o).orElse(null);
            } catch (RuntimeException rte) {
                log.warn(rte.getMessage());
                return null;
            }
        }
        return o;
    }

    protected static Method getBuilderMethod(Class<?> builderClass, String name, Class<?> type) {
        Method candidate = null;
        for (Method method : builderClass.getMethods()) {
            if (method.getName().equals(name)) {
                if (method.getParameterCount() == 1) {
                    if (candidate == null) {
                        candidate = method;
                     } else {
                        if (method.getParameterTypes()[0].isAssignableFrom(type)) {
                            candidate = method;
                        }
                    }
                }
            }

        }
        return candidate;

    }

    public static <T, S> S fillAndCatch(Provider<T> provider, S builder) {
        try {
            return fill(provider, builder);
        } catch (IllegalAccessException | InvocationTargetException | ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

}
