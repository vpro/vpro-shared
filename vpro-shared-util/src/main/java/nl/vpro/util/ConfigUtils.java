package nl.vpro.util;

import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Parameter;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.WildcardType;
import java.time.Duration;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.lang3.LocaleUtils;
import org.apache.commons.lang3.text.StrSubstitutor;

/**
 * @author Michiel Meeuwissen
 * @since 1.75
 */
@Slf4j
public class ConfigUtils {

    public static String[] getConfigFilesInHome(String... configFiles) {
        return Stream.concat(
            Arrays.stream(configFiles).map(c -> "classpath:/" + c),
            Arrays.stream(configFiles).map(c -> System.getProperty("user.home") + File.separator + "conf" + File.separator + c)
        ).toArray(String[]::new);
    }

    public static Map<String, String> getProperties(String... configFiles) throws IOException {
        return getProperties(new HashMap<>(), configFiles);
    }

    public static Map<String, String> getProperties(Map<String, String> initial, String... configFiles) throws IOException {
        return getProperties(initial, (s) -> s, configFiles);
    }
    public static <K> Map<K, String> getProperties(Map<K, String> initial, Function<String, K> keyGenerator, String... configFiles) throws IOException {


        Properties properties = new Properties();
        for (String configFile : configFiles) {
            if (configFile.startsWith("classpath:")) {
                InputStream in = ReflectionUtils.class.getResourceAsStream(configFile.substring("classpath:".length()));
                if (in != null) {
                    log.info("Reading properties from classpath {}", configFile);
                    properties.load(in);
                    continue;
                }
            }
            File file = new File(configFile);
            if (! file.exists()) {
                log.debug("The file {} does  not exists", file);
            } else if (! file.canRead()) {
                log.info("The file {} cannot be read", file);
            } else {
                log.info("Reading properties from {}", file);
                properties.load(new FileInputStream(file));
            }
        }
        for (Map.Entry<Object, Object> entry : properties.entrySet()) {
            initial.put(keyGenerator.apply(String.valueOf(entry.getKey())), String.valueOf(entry.getValue()));
        }
        substitute(initial);
        return Collections.unmodifiableMap(initial);
    }

    public static Map<String, String> getPropertiesInHome(String... configFiles) throws IOException {
        return getProperties(getConfigFilesInHome(configFiles));

    }

    public static <K> void substitute(Map<K, String> map) {
        StrSubstitutor subst = new StrSubstitutor(map.entrySet().stream()
                .collect(Collectors.toMap(Object::toString, Map.Entry::getValue)));

        for (Map.Entry<K, String> e : map.entrySet()) {
            e.setValue(subst.replace(e.getValue()));
        }
    }

    public static Map<String, String> filtered(Env e, Map<String, String> properties) {
        return filtered(e, null, properties);
    }

    public static Map<String, String> filtered(Env e, String prefix, Map<String, String> properties) {
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

    static Env getEnv(Map<String, String> properties) {
        return Env.valueOf(
            System.getProperty("env", // system prop
                properties.getOrDefault("env" // if not set default to set in config file
                    , "test") // if that not too, default to 'test'
            ).toUpperCase());
    }

    static <T> Object convert(String o, Parameter parameter) {
        return convert(o, parameter.getParameterizedType());
    }

    private static <T> Object convert(String o, Type parameterType)  {
        Class<?> parameterClass;
        if (parameterType instanceof  Class) {
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
