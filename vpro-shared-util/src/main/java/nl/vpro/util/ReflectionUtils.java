package nl.vpro.util;

import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.lang.reflect.*;
import java.time.Duration;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.apache.commons.lang3.LocaleUtils;

/**
 * @author Michiel Meeuwissen
 * @since 0.40
 */
@Slf4j
public class ReflectionUtils {

    public static Function<String, String> SETTER = k -> "set" + Character.toUpperCase(k.charAt(0)) + k.substring(1);
    public static Function<String, String> IDENTITY = k -> k;


    /**
     * Sets a certain property value in an object using reflection
     */

    public static void setProperty(Object instance, String key, Object value) {
        setProperty(instance, Arrays.asList(SETTER.apply(key), IDENTITY.apply(key)), value);
    }

    /**
     * Configure an instance using a map of properties.
     * @param setterName How, given a property name the setter methods must be calculated.
     */
    public static <T> T  configured(T instance, Map<String, String> properties, Collection<Function<String, String>> setterName) {
        log.debug("Configuring with {}", properties);
        properties.forEach((k, v) -> setProperty(instance,
            setterName.stream().map(f -> f.apply(String.valueOf(k))).collect(Collectors.toList()), v));
        return instance;
    }


    /**
     * Defaulting version of {@link #configured(Object, Map, Collection)}. Using {@link #SETTER}, {@link #IDENTITY} for the setter discovery.

     */
    public static <T> T configured(T instance, Map<String, String> properties) {
        return configured(instance, properties, Arrays.asList(SETTER, IDENTITY));
    }

    /**
     * We can also create the instance itself (supporting the Builder pattern of lombok)
     * @param clazz
     * @param properties
     * @param <T>
     * @return
     */
    public static <T> T configured(Class<T> clazz, Map<String, String> properties) {
        try {
            Method builderMethod = clazz.getDeclaredMethod("builder");
            if (Modifier.isStatic(builderMethod.getModifiers())) {
                Object builder = builderMethod.invoke(null);
                configured(builder, properties, Collections.singletonList(IDENTITY));
                Method objectMethod = builder.getClass().getMethod("build");
                return (T) objectMethod.invoke(builder);
            }
            log.debug("No static builder method found");
        } catch (NoSuchMethodException | InvocationTargetException | IllegalAccessException e) {
            log.debug("Canot build with builder because ", e);
        }
        try {
            Constructor<T> constructor = clazz.getConstructor();
            constructor.setAccessible(true);
            T object = constructor.newInstance();
            configured(object, properties);
            return object;
        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException | InstantiationException e) {
            log.error(e.getMessage(), e);
            throw new RuntimeException(e);
        }
    }


    @Deprecated
    public static <T> T configured(Env env, Class<T> clazz, String... configFiles) {
        return ConfigUtils.configured(env, clazz, configFiles);

    }


    @Deprecated
    public static void configured(Env env, Object instance, String... configFiles) {
        ConfigUtils.getProperties();
    }

    @Deprecated
    public static <T> T configured(Class<T> clazz, String... configfiles) {
        return ConfigUtils.configured(clazz, configfiles);
    }

    @Deprecated
    public static Map<String, String> getProperties(String... configFiles) throws IOException {
        return ConfigUtils.getProperties(configFiles);
    }

    @Deprecated
    public static Map<String, String> getProperties(Map<String, String> initial, String... configFiles) throws IOException {
        return ConfigUtils.getProperties(initial, configFiles);
    }

    @Deprecated
    public static void substitute(Map<String, String> map) {
        ConfigUtils.substitute(map, map);
    }

    @Deprecated
    public static Map<String, String> filtered(Env e, Map<String, String> properties) {
        return ConfigUtils.filtered(e, properties);
    }

    @Deprecated
    public static Map<String, String> filtered(Env e, String prefix,  Map<String, String> properties) {
        return ConfigUtils.filtered(e, prefix, properties);

    }


    @Deprecated
    public static <T> T configured(T instance, String... configFiles) {
        return ConfigUtils.configured(instance, configFiles);
    }

    @Deprecated
    public static <T> T configuredInHome(T instance, String... configFiles) {
        return ConfigUtils.configured(instance, ConfigUtils.getConfigFilesInHome(configFiles));
    }

    @Deprecated
    public static <T> T configuredInHome(Env env, T instance, String... configFiles) {
        return ConfigUtils.configuredInHome(env, instance, configFiles);
    }
    @Deprecated
    public static <T> T configured(Env env, Class<T> clazz, Map<String, String> config) {
        return ConfigUtils.configured(env, clazz, config);
    }

    @Deprecated
    public static <T> T configured(Env env, T instance, Map<String, String> config) {
        return ConfigUtils.configured(env, instance, config);
    }



    private static boolean setProperty(Object instance, Collection<String> setterNames, Object value) {
        Method[] methods = instance.getClass().getMethods();
        String v = (String) value;
        Parameter parameterClass = null;
        for (Method m : methods) {
            if (setterNames.contains(m.getName()) && m.getParameterCount() == 1) {
                try {
                    parameterClass = m.getParameters()[0];
                    m.invoke(instance, convert(v, parameterClass));
                    log.debug("Set {}#{} to {} from config file", instance, m.getName(), v);
                    return true;
                } catch (IllegalAccessException | InvocationTargetException e) {
                    log.error(e.getMessage(), e);
                }
            }
        }
        if (parameterClass != null) {
            log.warn("Unrecognized parameter type " + parameterClass);
        }
        log.debug("Unrecognized property {} on {}", setterNames, instance.getClass());
        return false;
    }

    static <T> Object convert(String o, Parameter parameter) {
        return convert(o, parameter.getParameterizedType());
    }

    private static <T> Object convert(String o, Type parameterType) {
        Class<?> parameterClass;
        if (parameterType instanceof Class) {
            parameterClass = (Class) parameterType;
        } else if (parameterType instanceof ParameterizedType) {
            parameterClass = (Class) ((ParameterizedType) parameterType).getRawType();
        } else if (parameterType instanceof WildcardType) {
            parameterClass = (Class) ((WildcardType) parameterType).getUpperBounds()[0];
        } else {
            throw new UnsupportedOperationException("Cannot convert " + o + " to " + parameterType);
        }
        if (String.class.isAssignableFrom(parameterClass)) {
            return o;
        } else if (boolean.class.equals(parameterClass) || parameterClass.isAssignableFrom(Boolean.class)) {
            return Boolean.valueOf(o);
        } else if (int.class.equals(parameterClass) || parameterClass.isAssignableFrom(Integer.class)) {
            return Integer.valueOf(o);
        } else if (long.class.equals(parameterClass) || parameterClass.isAssignableFrom(Long.class)) {
            return Long.valueOf(o);
        } else if (float.class.equals(parameterClass) || parameterClass.isAssignableFrom(Float.class)) {
            return Float.valueOf(o);
        } else if (double.class.equals(parameterClass) || parameterClass.isAssignableFrom(Double.class)) {
            return Double.valueOf(o);
        } else if (Enum.class.isAssignableFrom(parameterClass)) {
            try {
                return Enum.valueOf((Class<? extends Enum>) parameterClass, o);
            } catch (IllegalArgumentException iae) {
                return Enum.valueOf((Class<? extends Enum>) parameterClass, o.toUpperCase());
            }
        } else if (parameterClass.isAssignableFrom(Locale.class)) {
            return LocaleUtils.toLocale(o);
        } else if (parameterClass.isAssignableFrom(Duration.class)) {
            return TimeUtils.parseDuration(o).orElse(null);
        } else if (parameterClass.isAssignableFrom(List.class)) {
            ParameterizedType parameterizedType = (ParameterizedType) parameterType;
            return Arrays.stream(o.split("\\s*,\\s*"))
                .map(s -> convert(s, parameterizedType.getActualTypeArguments()[0]))
                .collect(Collectors.toList());
        } else {
            throw new UnsupportedOperationException();
        }
    }

}
