package nl.vpro.util;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.lang.reflect.*;
import java.time.Duration;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.apache.commons.lang3.LocaleUtils;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

import static nl.vpro.util.ReflectionUtils.ResultAction.*;

/**
 *
 * This provides some basic reflection code to call setters and getters on java beans. There are of course other libraries available
 * for these kind of things, but it is not hard,  and this allows for customizing details of it.
 * <p>
 * Most importantly we add a number of {@link #configured(Object, Map)} methods so this class together with {@link ConfigUtils} provides a complete configuration mechanism.
 * <p>
 *
 * @author Michiel Meeuwissen
 * @since 0.40
 */
@Slf4j
public class ReflectionUtils {

    /**
     * A normal 'bean' like setter of a property. The property 'title' is set by {@code setTitle(String}}
     */
    public static final Function<String, String> SETTER = ReflectionUtils::defaultSetter;
    /**
     * A normal 'bean' like getter of a property. The property 'title' is gotten by {@code getTitle()}
     */
    public static final Function<String, String> GETTER = ReflectionUtils::defaultGetter;

     /**
     * A builder like setter of a property. The property 'title' is set by {@code title(String)}
     */
    public static final Function<String, String> IDENTITY = k -> k;

    public static String defaultSetter(String k) {
        return "set" + Character.toUpperCase(k.charAt(0)) + k.substring(1);
    }


    public static String defaultGetter(String k) {
        return "get" + Character.toUpperCase(k.charAt(0)) + k.substring(1);
    }


    /**
     * Sets a certain property value in an object using reflection
     * @return A {@link Result} object describing what happened
     */
    public static Result setProperty(Object instance, String key, Object value) {
        return setProperty(
            instance,
            key,
            Arrays.asList(SETTER.apply(key), IDENTITY.apply(key)), value);
    }

    /**
     * Configure an instance using a map of properties.
     * @param setterName How, given a property name, the setter methods must be calculated.
     *                   This is list of functions to convert the name of a property to a setter-method.
     *
     */
    public static <T> T  configured(
        T instance,
        Map<String, String> properties,
        Collection<Function<String, String>> setterName) {
        log.debug("Configuring with {}", properties);
        final Set<String> found = new HashSet<>();
        final Set<String> notfound = new HashSet<>();
        properties.forEach(
            (k, v) -> {
                List<String> setterNames = setterName.stream()
                    .map(f -> f.apply(String.valueOf(k)))
                    .collect(Collectors.toList());
                if (setProperty(instance, k, setterNames, v).getAction() == SET) {
                    if (! found.add(k)) {
                        log.warn("{} Set twice!", k);
                    }
                } else {
                    notfound.add(k);
                }
            }
        );
        log.debug("Set {}/{}. Not found {}", found.size(), properties.size(), notfound);
        return instance;
    }

    /**
     * Configure an instance using a map of properties. Properties are only set if they are in the given instance still {@code null}.
     *
     * @param setterName How, given a property name, the setter methods must be calculated.
     * @param getterName How, given a property name, the getter methods must be calculated. Needed to check if the current value indeed is still {@code null}
     *
     */
    public static <T> T configureIfNull(@NonNull T instance,
                                        @NonNull Map<String, String> properties,
                                        @NonNull Collection<Function<String, String>> setterName,
                                        @NonNull Collection<Function<String, String>> getterName) {
        log.debug("Configuring with {}", properties);
        final Set<String> found = new HashSet<>();
        final Set<String> notfound = new HashSet<>();
        properties.forEach(
            (k, v) -> {
                List<String> setterNames = setterName.stream()
                    .map(f -> f.apply(String.valueOf(k)))
                    .collect(Collectors.toList());
                List<String> getterNames = getterName.stream()
                    .map(f -> f.apply(String.valueOf(k)))
                    .collect(Collectors.toList());
                ResultAction result = setProperty(instance, k, setterNames, getterNames, v, true).getAction();
                if (result == SET) {
                    if (!found.add(k)) {
                        log.warn("{} Set twice!", k);
                    }
                } else {
                    if (result.isErroneous()) {
                        notfound.add(k);
                    }
                }
            }
        );
        log.debug("Set {}/{}. Not found {}", found.size(), properties.size(), notfound);
        return instance;
    }


    /**
     * Defaulting version of {@link #configured(Object, Map, Collection)}.
     * Using {@link #SETTER}, {@link #IDENTITY} for the setter discovery, which means that normal bean like setters and builder pattern setters are supported.
     * <p>
     * E.g. if the given properties contain 'title=foo bar' then the code will try {@code setTitle("foo bar")}, or if this method does not exist {@code title("foo bar"}}

     */
    public static <T> T configured(T instance, Map<String, String> properties) {
        return configured(instance, properties, Arrays.asList(SETTER, IDENTITY));
    }

    /**
     * Defaulting version of {@link #configureIfNull(Object, Map, Collection, Collection)} Using {@link #SETTER}, {@link #IDENTITY} for the setter discovery, and {@link #GETTER}, {@link #IDENTITY} for getter.
     */
    @SuppressWarnings("UnusedReturnValue")
    public static <T> T configureIfNull(T instance, Map<String, String> properties) {
        return configureIfNull(instance, properties,
            Arrays.asList(SETTER, IDENTITY),
            Arrays.asList(GETTER, IDENTITY)
        );
    }


    /**
     * We can also create the instance itself (supporting the Builder pattern of lombok)
     * @param clazz Class which can have a static 'builder' method, or a no args constructor
     * @param properties Properties which are used to configure the created instant
     * @param <T> type of object to create
     * @return a new, configured instance of T
     */
    @SuppressWarnings("unchecked")
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
            log.debug("Cannot build with builder because ", e);
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


    /**
     * @deprecated Use {@link ConfigUtils}
     */
    @Deprecated
    public static <T> T configured(Env env, Class<T> clazz, String... configFiles) {
        return ConfigUtils.configured(env, clazz, configFiles);
    }


    /**
     * @deprecated Use {@link ConfigUtils}
     */
    @Deprecated
    public static void configured(Env env, Object instance, String... configFiles) {
        ConfigUtils.configured(env, instance, configFiles);
    }

     /**
     * @deprecated Use {@link ConfigUtils}
     */
    @Deprecated
    public static <T> T configured(Class<T> clazz, String... configfiles) {
        return ConfigUtils.configured(clazz, configfiles);
    }

    /**
     * @deprecated Use {@link ConfigUtils}
     */
    @Deprecated
    public static Map<String, String> getProperties(String... configFiles) {
        return ConfigUtils.getProperties(configFiles);
    }

     /**
     * @deprecated Use {@link ConfigUtils}
     */
    @Deprecated
    public static Map<String, String> getProperties(Map<String, String> initial, String... configFiles) {
        return ConfigUtils.getProperties(initial, configFiles);
    }

    @Deprecated
    public static void substitute(Map<String, String> map) {
        ConfigUtils.substitute(map, map);
    }

    /**
     * @deprecated Use {@link ConfigUtils}
     */
    @Deprecated
    public static Map<String, String> filtered(Env e, Map<String, String> properties) {
        return ConfigUtils.filtered(e, properties);
    }

    /**
     * @deprecated Use {@link ConfigUtils}
     */
    @Deprecated
    public static Map<String, String> filtered(Env e, String prefix,  Map<String, String> properties) {
        return ConfigUtils.filtered(e, prefix, properties);

    }


    /**
     * @deprecated Use {@link ConfigUtils}
     */
    @Deprecated
    public static <T> T configured(T instance, String... configFiles) {
        return ConfigUtils.configured(instance, configFiles);
    }

    /**
     * @deprecated Use {@link ConfigUtils}
     */
    @Deprecated
    public static <T> T configuredInHome(T instance, String... configFiles) {
        return ConfigUtils.configured(instance, ConfigUtils.getConfigFilesInHome(configFiles));
    }

    /**
     * @deprecated Use {@link ConfigUtils}
     */
    @Deprecated
    public static <T> T configuredInHome(Env env, T instance, String... configFiles) {
        return ConfigUtils.configuredInHome(env, instance, configFiles);
    }

    /**
     * @deprecated Use {@link ConfigUtils}
     */
    @Deprecated
    public static <T> T configured(Env env, Class<T> clazz, Map<String, String> config) {
        return ConfigUtils.configured(env, clazz, config);
    }

    /**
     * @deprecated Use {@link ConfigUtils}
     */
    @Deprecated
    public static <T> T configured(Env env, T instance, Map<String, String> config) {
        return ConfigUtils.configured(env, instance, config);
    }


    private static Result  setProperty(
        @NonNull Object instance,
        @Nullable String fieldName,
        @NonNull Collection<String> setterNames,
        @NonNull Collection<String> getterNames,
        @Nullable Object value, boolean onlyIfNull) {
        String v = value == null ? null : String.valueOf(value);

        Method[] methods = instance.getClass().getMethods();
        Type parameterClass = null;
        if (onlyIfNull) {
            for (Method m : methods) {
                if (getterNames.contains(m.getName()) && m.getParameterCount() == 0) {
                    try {
                        Object existingValue = m.invoke(instance);
                        if (existingValue != null) {
                            return new Result(fieldName, IGNORED);
                        }
                    } catch (IllegalAccessException | InvocationTargetException e) {
                        log.error(e.getMessage(), e);
                    }
                }
            }
            if (fieldName != null) {
                try {
                    Field f = getField(instance.getClass(), fieldName);
                    f.setAccessible(true);
                    Object existingValue = f.get(instance);
                    if (existingValue != null) {
                        return new Result(fieldName, IGNORED);
                    }
                } catch (NoSuchFieldException e) {
                    log.debug(e.getMessage());
                } catch (IllegalAccessException e) {
                    log.error(e.getMessage(), e);
                }
            }
        }
        for (Method m : methods) {
            if (setterNames.contains(m.getName()) && m.getParameterCount() == 1) {
                try {
                    parameterClass = m.getParameters()[0].getParameterizedType();
                    Object convertedValue = convert(v, parameterClass);
                    m.invoke(instance, convertedValue);
                    log.debug("Set {} to {}", m.getName(), v);
                    return new Result(fieldName, SET);
                } catch (IllegalAccessException | InvocationTargetException e) {
                    log.error(e.getMessage(), e);
                }
            }
        }
        if (fieldName != null) {
            try {
                Field f = getField(instance.getClass(), fieldName);
                parameterClass = f.getType();
                f.setAccessible(true);
                f.set(instance, convert(v, f.getGenericType()));
                log.debug("Set field {} to {}", f.getName(), v);
                return  new Result(fieldName, SET);
            } catch (NoSuchFieldException e) {
                log.debug(e.getMessage());
            } catch (IllegalAccessException e) {
                log.error(e.getMessage(), e);
            }
        }
        if (parameterClass != null) {
            log.warn("Unrecognized parameter type " + parameterClass);
        }
        log.debug("Unrecognized property {} on {}", setterNames, instance.getClass());
        return new Result(fieldName, NOTFOUND);
    }

    private static Result setProperty(Object instance, String fieldName, Collection<String> setterNames, Object value) {
        return setProperty(instance, fieldName, setterNames, Collections.emptyList(), value, false);
    }

    static <T> Object convert(String o, Parameter parameter) {
        return convert(o, parameter.getParameterizedType());
    }

    @SuppressWarnings({"unchecked", "ConstantConditions"})
    private static <T> Object convert(String o, Type parameterType) {
        Class<?> parameterClass;
        if (parameterType instanceof Class) {
            parameterClass = (Class) parameterType;
        } else if (parameterType instanceof ParameterizedType parameterizedType) {
            parameterClass = (Class) parameterizedType.getRawType();
        } else if (parameterType instanceof WildcardType wildcardType) {
            parameterClass = (Class) wildcardType.getUpperBounds()[0];
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
                try {
                    return Enum.valueOf((Class<? extends Enum>) parameterClass, o.toUpperCase());
                } catch (IllegalArgumentException iae2) {
                    try {
                        Method valueOfXml = parameterClass.getDeclaredMethod("valueOfXml", String.class);
                        valueOfXml.setAccessible(true);
                        return valueOfXml.invoke(null, o);
                    } catch (IllegalAccessException | InvocationTargetException | NoSuchMethodException nsme) {
                        throw iae2;
                    }
                }
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

    /**
     * Returns the first {@link Field} in the hierarchy for the specified name
     */
    private static Field getField(@NonNull Class<?> clazz, final @NonNull String name) throws NoSuchFieldException {
        NoSuchFieldException noSuchFieldException = null;
        while (clazz != null) {
            try {
                return clazz.getDeclaredField(name);
            } catch (NoSuchFieldException e) {
                noSuchFieldException = e;
            }
            clazz = clazz.getSuperclass();
        }
        throw noSuchFieldException;
    }

    public static boolean hasClass(String clazz) {
        try {
            classForName(clazz, ReflectionUtils.class.getClassLoader());
            return true;
        } catch (ClassNotFoundException e) {
            log.debug(e.getMessage(), e);
            return false;
        }

    }

    /**
     * Finds the class given its name.
     * <br>
     * This method also retrieves primitive types (unlike {@code Class#forName(String)}).
     */
    public static Class<?> classForName(String name, ClassLoader loader) throws ClassNotFoundException {
        Class<?> c = primitiveClasses.get(name);
        if (c == null) {
            c = Class.forName(name, false, loader);
        }
        return c;
    }

    public static Class<?> classForName(String name) throws ClassNotFoundException {
        return classForName(name, ReflectionUtils.class.getClassLoader());
    }

    @SuppressWarnings("unchecked")
    public static <T> T callProtected(Object o, Class<?> clazz, String method) throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        Method   m = clazz.getDeclaredMethod("getPredictionsForXml");
        m.setAccessible(true);
        T t = (T) m.invoke(o);
        return t;
    }

    public static <T> T callProtected(Object o, String method) throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        Class<?> clazz = o.getClass();
        while(! clazz.equals(Object.class)) {
            try {
                return callProtected(o, clazz, method);
            } catch (NoSuchMethodException nsm) {
                clazz = clazz.getSuperclass();
            }
        }
        throw new NoSuchMethodException();
    }








    private static final Map<String, Class<?>> primitiveClasses = new HashMap<>();
    static {
        Class<?>[] primitives = {byte.class, short.class, int.class, long.class, float.class, double.class, char.class, boolean.class};
        for (Class<?> clazz : primitives)
            primitiveClasses.put(clazz.getName(), clazz);
    }

    @Getter
    public static class Result {
        final String property;
        final ResultAction action;

        public Result(String property, ResultAction action) {
            this.property = property;
            this.action = action;
        }
    }

    @Getter
    public enum ResultAction {
        SET(false),
        NOTFOUND(true),
        ERROR(true),
        IGNORED(false);
        final boolean erroneous;

        ResultAction(boolean erroneous) {
            this.erroneous = erroneous;
        }
    }

}
