package nl.vpro.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

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
            Stream.concat(
                Arrays.stream(configFiles).map(c -> "classpath:/" + c),
                Arrays.stream(configFiles).map(c -> System.getProperty("user.home") + File.separator + "conf" + File.separator + c)
            ).toArray(String[]::new)
        );
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
        LOG.info("Running in {}", e);
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
        Class<?> parameterClass = null;
        for (Method m : methods) {
            if (setterNames.contains(m.getName()) && m.getParameterCount() == 1) {
                try {
                    parameterClass = m.getParameters()[0].getType();
                    if (String.class.isAssignableFrom(parameterClass)) {
                        m.invoke(instance, v);
                    } else if (boolean.class.isAssignableFrom(parameterClass) || Boolean.class.isAssignableFrom(parameterClass)) {
                        m.invoke(instance, Boolean.valueOf(v));
                    } else if (int.class.isAssignableFrom(parameterClass) || Integer.class.isAssignableFrom(parameterClass)) {
                        m.invoke(instance, Integer.valueOf(v));
                    } else if (long.class.isAssignableFrom(parameterClass) || Long.class.isAssignableFrom(parameterClass)) {
                        m.invoke(instance, Long.valueOf(v));
                    } else if (float.class.isAssignableFrom(parameterClass) || Float.class.isAssignableFrom(parameterClass)) {
                        m.invoke(instance, Float.valueOf(v));
                    } else if (double.class.isAssignableFrom(parameterClass) || Double.class.isAssignableFrom(parameterClass)) {
                        m.invoke(instance, Double.valueOf(v));
                    } else {
                        LOG.debug("Unrecognized parameter type " + parameterClass);
                        continue;
                    }
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

}
