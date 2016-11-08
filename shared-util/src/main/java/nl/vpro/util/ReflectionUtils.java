package nl.vpro.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.*;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.lang3.LocaleUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Maps;

/**
 * @author Michiel Meeuwissen
 * @since 0.40
 */
public class ReflectionUtils {


    private static Logger LOG = LoggerFactory.getLogger(ReflectionUtils.class);

    public static Function<String, String> SETTER = k -> "set" + Character.toUpperCase(k.charAt(0)) + k.substring(1);
    public static Function<String, String> IDENTITY = k -> k;


    public static void setProperty(Object instance, String key, Object value) {
        setProperty(instance, Arrays.asList(SETTER.apply(key), IDENTITY.apply(key)), value);
    }

    public static <T> T configured(T instance, String... configFiles) {
        return configured(null, instance, configFiles);
    }

    public static <T> T configuredInHome(T instance, String... configFiles) {
        return configuredInHome(null, instance, configFiles);
    }

    public static <T> T configuredInHome(Env env, T instance, String... configFiles) {
        return configured(
            env,
            instance,
            getConfigFilesInHome(configFiles)
        );
    }

    public static String[] getConfigFilesInHome(String... configFiles) {
        return Stream.concat(
            Arrays.stream(configFiles).map(c -> "classpath:/" + c),
            Arrays.stream(configFiles).map(c -> System.getProperty("user.home") + File.separator + "conf" + File.separator + c)
        ).toArray(String[]::new);
    }

    public static <T> T configured(Env env, T instance, String... configFiles) {
        try {
            Map<String, String> properties = getProperties(configFiles);
            if (env == null) {
                env = getEnv(properties);
            }
            return configured(env, instance, properties);
        } catch (IOException ioe) {
            throw new RuntimeException(ioe);
        }
    }


    public static <T> T  configured(Env env, T instance, Map<String, String> properties, Collection<Function<String, String>> setterName) {
        Map<String, String> filtered = filtered(env, null, properties);
        LOG.debug("Configuring with {}", filtered);
        filtered.forEach((k, v) -> ReflectionUtils.setProperty(instance,
            setterName.stream().map(f -> f.apply(String.valueOf(k))).collect(Collectors.toList()), v));
        return instance;
    }

    public static <T> T configured(Env env, T instance, Map<String, String> properties) {
        return configured(env, instance, properties, Arrays.asList(SETTER, IDENTITY));
    }


    public static <T> T configured(T instance, Map<String, String> properties) {
        return configured(getEnv(properties), instance, properties, Arrays.asList(SETTER, IDENTITY));
    }


    public static <T> T configured(Env env, Class<T> clazz, Map<String, String> properties) {
        try {
            Method builderMethod = clazz.getDeclaredMethod("builder");
            if (Modifier.isStatic(builderMethod.getModifiers())) {
                Object builder = builderMethod.invoke(null);
                configured(env, builder, properties, Collections.singletonList(IDENTITY));
                Method objectMethod = builder.getClass().getMethod("build");
                return (T) objectMethod.invoke(builder);
            }
            LOG.debug("No static builder method found");
        } catch (NoSuchMethodException | InvocationTargetException | IllegalAccessException e) {
            LOG.debug("Canot build with builder because ", e);
        }
        try {
            Constructor<T> constructor = clazz.getConstructor();
            constructor.setAccessible(true);
            T object = constructor.newInstance();
            configured(env, object, properties);
            return object;
        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException | InstantiationException e) {
            LOG.error(e.getMessage(), e);
            throw new RuntimeException(e);
        }
    }

    public static <T> T configured(Env env, Class<T> clazz, String... configFiles) {
        try {
            return configured(env, clazz, getProperties(configFiles));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static <T> T configured(Class<T> clazz, Map<String, String> properties) {
        return configured(getEnv(properties), clazz, properties);
    }

    public static <T> T configured(Class<T> clazz, String... configfiles) {
        try {
            Map<String, String> properties = getProperties(configfiles);
            return configured(getEnv(properties), clazz, properties);
        } catch (IOException e) {
            LOG.error(e.getMessage(), e);
            throw new RuntimeException(e);
        }
    }


    public static Map<String, String> getProperties(String... configFiles) throws IOException {
        Properties properties = new Properties();
        for (String configFile : configFiles) {
            if (configFile.startsWith("classpath:")) {
                InputStream in = ReflectionUtils.class.getResourceAsStream(configFile.substring("classpath:".length()));
                if (in != null) {
                    LOG.info("Reading properties from classpath {}", configFile);
                    properties.load(in);
                    continue;
                }
            }
            File file = new File(configFile);
            if (!file.canRead()) {
                LOG.info("The file {} cannot be read", file);
            } else {
                LOG.info("Reading properties from {}", file);
                properties.load(new FileInputStream(file));
            }
        }
        return Maps.fromProperties(properties);
    }

    public static Map<String, String> filtered(Env e, Map<String, String> properties) {
        return filtered(e, null, properties);
    }

    public static Map<String, String> filtered(Env e, String prefix,  Map<String, String> properties) {
        if (e == null) {
            if (System.getProperty("env") != null) {
                e = Env.valueOf(System.getProperty("env").toUpperCase());
            }
        }
        final Env env = e;
        LOG.info("Configuring {} in {}", prefix, e);
        Map<String, String> result = new HashMap<>();
        properties.forEach((key, value) -> {
            String[] split = key.split("\\.", 3);
            if (split.length == 1) {
                String explicitValue = value;
                if (env != null) {
                    String envValue = properties.get(key + "." + env);
                    if (envValue != null) {
                        explicitValue = envValue;
                    }
                }
                result.put(key, explicitValue);
            }
        });
        properties.forEach((key, value) -> {
            String[] split = key.split("\\.", 3);
            if (split.length == 3) {
                if (!Objects.equals(prefix, split[0])) {
                    return;
                }
                split = Arrays.copyOfRange(split, 1, 3);
            }
            if (split.length > 1 ) {
                if ((env == null && split[1].equals("test" ) && !result.containsKey(split[0])) || (env != null && split[1].toUpperCase().equals(env.name()))) {
                    result.put(split[0], value);
                }
            }

        });
        return result;
    }


    private static Env getEnv(Map<String, String> properties) {
        return Env.valueOf(System.getProperty("env", properties.getOrDefault("env", "test")).toUpperCase());
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
                    LOG.debug("Set {} from config file", m.getName());
                    return true;
                } catch (IllegalAccessException | InvocationTargetException e) {
                    LOG.error(e.getMessage(), e);
                }
            }
        }
        if (parameterClass != null) {
            LOG.warn("Unrecognized parameter type " + parameterClass);
        }
        LOG.debug("Unrecognized property {} on {}", setterNames, instance.getClass());
        return false;
    }

    private static <T> Object convert(String o, Parameter parameter) {
        return convert(o, parameter.getParameterizedType());
    }
    private static <T> Object convert(String o, Type parameterType)  {
        Class<?> parameterClass;
        if (parameterType instanceof  Class) {
            parameterClass = (Class) parameterType;
        } else if (parameterType instanceof ParameterizedType) {
            parameterClass = (Class) ((ParameterizedType) parameterType).getRawType();
        } else {
            throw new UnsupportedOperationException();
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
        } else if (parameterClass.isAssignableFrom(Locale.class)) {
            return LocaleUtils.toLocale(o);
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
