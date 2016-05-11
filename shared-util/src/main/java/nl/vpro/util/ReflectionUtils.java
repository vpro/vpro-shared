package nl.vpro.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Michiel Meeuwissen
 * @since 0.40
 */
public class ReflectionUtils {

    private static Logger LOG = LoggerFactory.getLogger(ReflectionUtils.class);


    public static void setProperty(Object instance, Object key, Object value) {
        Method[] methods = instance.getClass().getMethods();
        String k = (String) key;
        String v = (String) value;
        String setter = "set" + Character.toUpperCase(k.charAt(0)) + k.substring(1);
        for (Method m : methods) {
            if (m.getName().equals(setter) && m.getParameterCount() == 1) {
                try {
                    Class<?> parameterClass = m.getParameters()[0].getType();
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
                        LOG.warn("Unrecognized parameter type");
                        continue;
                    }
                    LOG.debug("Set {} from config file", key);
                    return;
                } catch (IllegalAccessException | InvocationTargetException e) {
                    LOG.error(e.getMessage(), e);
                }
            }

        }
        LOG.error("Unrecognized property {} on {}", key, instance.getClass());
    }

    public static void configured(Object instance, String... configFiles) throws IOException {
        configured(null, instance, configFiles);
    }

    public static void configured(String env, Object instance, String... configFiles) throws IOException {
        Properties properties = filtered(env, getProperties(configFiles));
        LOG.debug("Configuring with {}", properties);
        properties.forEach((k, v) -> ReflectionUtils.setProperty(instance, k, v));
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
    public static  Properties filtered(String env, Properties properties) {
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
            } else {
                if ((env == null && split[1].equals("test")) || split[1].equals(env)) {
                    result.put(split[0], value);
                }
            }
        });
        return result;
    }
}
