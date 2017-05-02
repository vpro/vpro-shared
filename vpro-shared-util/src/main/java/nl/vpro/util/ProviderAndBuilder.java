package nl.vpro.util;

import lombok.extern.slf4j.Slf4j;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.util.Optional;

import javax.inject.Provider;

/**
 * If you have a provider implementation based on a builder, you can fill the fields of the provider to the builder using
 * reflection.
 *
 * @author Michiel Meeuwissen
 * @since 1.69
 */
@Slf4j
public class ProviderAndBuilder {


    public static <T, S> S fill(Provider<T> provider, S builder) throws NoSuchMethodException, InvocationTargetException, IllegalAccessException, ClassNotFoundException {
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
            try {
                Method builderMethod = builderClass.getMethod(providerField.getName(), providerType);
                builderMethod.invoke(builder, providerValue);
            } catch (NoSuchMethodException nsm) {
                log.info("Ignored {}", providerField);
            }
        }
        return builder;
    }

    public static <T, S> S fillAndCatch(Provider<T> provider, S builder) {
        try {
            return fill(provider, builder);
        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException | ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

}
