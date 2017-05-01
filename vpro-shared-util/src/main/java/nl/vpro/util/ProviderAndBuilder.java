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
        Class<?> clazz = provider.getClass();
        Class<?> builderClass = builder.getClass();
        for (Field f : clazz.getDeclaredFields()) {
            f.setAccessible(true);
            Class<?> fieldType = f.getType();
            if (fieldType.equals(builderClass)) {
                continue;
            }
            Object fieldValue = f.get(provider);
            if (Optional.class.isAssignableFrom(fieldType)) {
                fieldType = Class.forName(((ParameterizedType) f.getGenericType()).getActualTypeArguments()[0].getTypeName());
                fieldValue = fieldValue == null ? null : ((Optional) fieldValue).orElse(null);
            }
            try {
                Method m = builderClass.getMethod(f.getName(), fieldType);
                m.invoke(builder, fieldValue);
            } catch (NoSuchMethodException nsm) {
                log.info("Ignored {}", f);
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
