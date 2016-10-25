package nl.vpro.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Properties;
import java.util.function.Function;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Michiel Meeuwissen
 * @since 0.40
 */
public class ReflectionUtils {


    private static Logger LOG = LoggerFactory.getLogger(ReflectionUtils.class);

    public static Function<String, String> SETTER = k -> "set" + Character.toUpperCase(k.charAt(0)) + k.substring(1);
    public static Function<String, String> IDENTITY = k -> k;


    public static void setProperty(Object instance, Object key, Object value, Function<String, String> setterName) {
        Method[] methods = instance.getClass().getMethods();
        String k = (String) key;
        String v = (String) value;
        String setter = setterName.apply(k);
        Class<?> parameterClass = null;
        for (Method m : methods) {
            if (m.getName().equals(setter) && m.getParameterCount() == 1) {
                try {
                    parameterClass = m.getParameters()[0].getType();
                    if (String.class.isAssignableFrom(parameterClass)) {
                        m.invoke(instance, v);
                    } else if (boolean.class.isAssignableFrom(parameterClass)) {
                        m.invoke(instance, Boolean.valueOf(v));
                    } else if (int.class.isAssignableFrom(parameterClass) || Integer.class.isAssignableFrom(parameterClass)) {
                        m.invoke(instance, Integer.valueOf(v));
                    } else if (long.class.isAssignableFrom(parameterClass) || Long.class.isAssignableFrom(parameterClass)) {
                        m.invoke(instance, Long.valueOf(v));
                    } else if (double.class.isAssignableFrom(parameterClass) || Double.class.isAssignableFrom(parameterClass)) {
                        m.invoke(instance, Double.valueOf(v));
                    } else {
                        LOG.debug("Unrecognized parameter type " + parameterClass);
                        continue;
                    }
                    LOG.debug("Set {} from config file", key);
                    return;
                } catch (IllegalAccessException | InvocationTargetException e) {
                    LOG.error(e.getMessage(), e);
                }
            }
        }
        if (parameterClass != null) {
            LOG.warn("Unrecognized parameter type " + parameterClass);
        }
        LOG.error("Unrecognized property {} on {}", key, instance.getClass());
    }

    public static void setProperty(Object instance, Object key, Object value) {
        setProperty(instance, key, value, SETTER);
    }

    public static void configured(Object instance, String... configFiles) {
        configured(null, instance, configFiles);
    }

    public static void configured(Env env, Object instance, String... configFiles) {
        try {
            Properties properties = getProperties(configFiles);
            configured(env, instance, properties);
        } catch (IOException ioe) {
            throw new RuntimeException(ioe);
        }
    }

    public static void configured(Env env, Object instance, Properties properties, Function<String, String> setterName) {
        Properties filtered = filtered(env, properties);
        LOG.debug("Configuring with {}", filtered);
        filtered.forEach((k, v) -> ReflectionUtils.setProperty(instance, k, v, setterName));
    }

    public static void configured(Env env, Object instance, Properties properties) {
        configured(env, instance, properties, SETTER);
    }

    public static <T> T lombok(Env env, Class<T> clazz, Properties properties) {
        try {
            Method builder = clazz.getDeclaredMethod("builder");
            if (Modifier.isStatic(builder.getModifiers())) {
                Object o = builder.invoke(null);
                configured(env, o, properties, IDENTITY);
                Method build = o.getClass().getMethod("build");
                return (T) build.invoke(o);
            }
            throw new RuntimeException("Cant build since no static builder method found in " +  clazz);
        } catch (NoSuchMethodException | InvocationTargetException | IllegalAccessException e) {
            throw new RuntimeException("Cant build ", e);
        }
    }

    public static <T> T lombok(Env env, Class<T> clazz, String... configFiles) {
        try {
            return lombok(env, clazz, getProperties(configFiles));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static Properties getProperties(String... configFiles) throws IOException {
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
        return properties;
    }
    public static  Properties filtered(Env e, Properties properties) {
        if (e == null) {
            if (System.getProperty("env") != null) {
                e = Env.valueOf(System.getProperty("env").toUpperCase());
            }
        }
        final Env env = e;
        Properties result = new Properties();
        properties.forEach((k, v) -> {
            String key = (String) k;
            String value = (String) v;
            String[] split = key.split("\\.", 2);
            if (split.length == 1) {
                String explicitValue = value;
                if (env != null) {
                    String envValue = properties.getProperty(key + "." + env);
                    if (envValue != null) {
                        explicitValue = envValue;
                    }
                }
                result.put(key, explicitValue);
            }
        });
        properties.forEach((k, v) -> {
                String key = (String) k;
                String value = (String) v;
                String[] split = key.split("\\.", 2);
                if (split.length > 1) {
                    if ((env == null && split[1].equals("test") && ! result.containsKey(split[0])) || (env != null && split[1].toUpperCase().equals(env.name()))) {
                        result.put(split[0], value);
                    }
                }
        });
        return result;
    }
}
