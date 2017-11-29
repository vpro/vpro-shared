package nl.vpro.util;

import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.lang.reflect.*;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * @author Michiel Meeuwissen
 * @since 0.40
 */
@Slf4j
public class ReflectionUtils {

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
            ConfigUtils.getConfigFilesInHome(configFiles)
        );
    }

    @Deprecated
    public static String[] getConfigFilesInHome(String... configFiles) {
        return ConfigUtils.getConfigFilesInHome(configFiles);
    }

    public static <T> T configured(Env env, T instance, String... configFiles) {
        try {
            Map<String, String> properties = ConfigUtils.getProperties(configFiles);
            if (env == null) {
                env = ConfigUtils.getEnv(properties);
            }
            return configured(env, instance, properties);
        } catch (IOException ioe) {
            throw new RuntimeException(ioe);
        }
    }


    public static <T> T  configured(Env env, T instance, Map<String, String> properties, Collection<Function<String, String>> setterName) {
        Map<String, String> filtered = ConfigUtils.filtered(env, null, properties);
        log.debug("Configuring with {}", filtered);
        filtered.forEach((k, v) -> ReflectionUtils.setProperty(instance,
            setterName.stream().map(f -> f.apply(String.valueOf(k))).collect(Collectors.toList()), v));
        return instance;
    }

    public static <T> T configured(Env env, T instance, Map<String, String> properties) {
        return configured(env, instance, properties, Arrays.asList(SETTER, IDENTITY));
    }


    public static <T> T configured(T instance, Map<String, String> properties) {
        return configured(ConfigUtils.getEnv(properties), instance, properties, Arrays.asList(SETTER, IDENTITY));
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
            log.debug("No static builder method found");
        } catch (NoSuchMethodException | InvocationTargetException | IllegalAccessException e) {
            log.debug("Canot build with builder because ", e);
        }
        try {
            Constructor<T> constructor = clazz.getConstructor();
            constructor.setAccessible(true);
            T object = constructor.newInstance();
            configured(env, object, properties);
            return object;
        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException | InstantiationException e) {
            log.error(e.getMessage(), e);
            throw new RuntimeException(e);
        }
    }

    public static <T> T configured(Env env, Class<T> clazz, String... configFiles) {
        try {
            return configured(env, clazz, ConfigUtils.getProperties(configFiles));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static <T> T configured(Class<T> clazz, Map<String, String> properties) {
        return configured(ConfigUtils.getEnv(properties), clazz, properties);
    }

    public static <T> T configured(Class<T> clazz, String... configfiles) {
        try {
            Map<String, String> properties = ConfigUtils.getProperties(configfiles);
            return configured(ConfigUtils.getEnv(properties), clazz, properties);
        } catch (IOException e) {
            log.error(e.getMessage(), e);
            throw new RuntimeException(e);
        }
    }


    @Deprecated
    public static Map<String, String> getProperties(String... configFiles) throws IOException {
        return ConfigUtils.getProperties(new HashMap<>(), configFiles);
    }

    @Deprecated
    public static Map<String, String> getProperties(Map<String, String> initial, String... configFiles) throws IOException {
        return ConfigUtils.getProperties(initial, configFiles);
    }

    @Deprecated
    public static void substitute(Map<String, String> map) {
        ConfigUtils.substitute(map);
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
        log.debug("Filtering{}for {}", prefix == null ? "" : prefix + " ", e);
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
                    m.invoke(instance, ConfigUtils.convert(v, parameterClass));
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


}
