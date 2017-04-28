package nl.vpro.util;

import lombok.AllArgsConstructor;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.BiPredicate;
import java.util.function.Function;

import javax.inject.Provider;

/**
 * If you have a provider implementation based on a builder, you can fill the fields of the provider to the builder using
 * reflection.
 *
 * @author Michiel Meeuwissen
 * @since 1.69
 */
public class ProviderAndBuilder {

    private static final List<Converter<?, ?>> CONVERTERS = new ArrayList<>();
    static {
        CONVERTERS.add(new Converter<>(s -> s, (o, c) -> c.isInstance(o)));
        CONVERTERS.add(new Converter<String, Duration>(
            s -> TimeUtils.parseDuration(s).orElse(null),
            (o, c) -> (String.class.isInstance(o) && c.equals(Duration.class))));
    }


    public static <T, S> S fill(Provider<T> provider, S builder) throws NoSuchMethodException, InvocationTargetException, IllegalAccessException, ClassNotFoundException {
        Class<?> clazz = provider.getClass();
        Class<?> builderClass = builder.getClass();
        for (Field f : clazz.getDeclaredFields()) {
            f.setAccessible(true);
            Class<?> fieldType = f.getType();
            Object fieldValue = f.get(provider);
            if (Optional.class.isAssignableFrom(fieldType)) {
                fieldType = Class.forName(((ParameterizedType) f.getGenericType()).getActualTypeArguments()[0].getTypeName());
                fieldValue = fieldValue == null ? null : ((Optional) fieldValue).orElse(null);
            }
            MethodAndConverter methodAndConverter = find
            while(true) {
                try {
                    Method m = builderClass.getMethod(f.getName(), fieldType);
                    m.invoke(builder, fieldValue);
                    break;
                } catch (NoSuchMethodException nsm) {

                }
            }

        }

        Method build = builderClass.getMethod("build");

        return builder;
    }

    public static <T, S> S fillAndCatch(Provider<T> provider, S builder) {
        try {
            return fill(provider, builder);
        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException | ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    private MethodAndConverter findMethod(Class clazz, String name, Class fieldType) throws NoSuchMethodException {
        Method m = clazz.getMethod(name, fieldType);
        return new MethodAndConverter(m, CONVERTERS.get(0));

    }

    @AllArgsConstructor
    private static class MethodAndConverter {
        final Method method;
        final Converter<?, ?> convertor;
    }

    @AllArgsConstructor
    public static class Converter<F, T> implements Function<F, T>, BiPredicate<Object, Class<?>> {
        private final Function<F, T> converter;
        private final BiPredicate<Object, Class<?>> applies;

        @Override
        public T apply(F f) {
            return converter.apply(f);

        }

        @Override
        public boolean test(Object o, Class<?> toClass) {
            return applies.test(o, toClass);

        }
    }
}
