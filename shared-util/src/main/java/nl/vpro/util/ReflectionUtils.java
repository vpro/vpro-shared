package nl.vpro.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
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
        Properties properties = new Properties();
        for (String configFile : configFiles) {
            File file = new File(configFile);
            if (!file.canRead()) {
                LOG.info("The file {} cannot be read", file);
            } else {
                LOG.info("Reading properties from {}", file);
                properties.load(new FileInputStream(file));
                properties.forEach((k, v) -> ReflectionUtils.setProperty(instance, k, v));
            }
        }
    }
}
