package nl.vpro.util;

import lombok.extern.slf4j.Slf4j;

import java.io.*;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Stream;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.text.StringSubstitutor;

/**
 * At VPRO we use an convention for configuring web-application using property-files.
 * This boils down to a fall back mechanism:
 * <ol>
 *     <li>Look in {@code {user.home}/conf/<application name>.properties}</li>
 *     <li>Look in {@code <classpath>/override-<applicationname>.properties}</li>
 *     <li>Look in {@code <classpath>/<applicationname>.properties}</li>
 * </ol>
 * This class provides utilities to arrange that. Also it is aware of {@link Env} (explicitely or via an 'env' property)  so that there is a consistent system for
 * varying properties across different OTAP deployments.
 *
 * I.e. it provides the means to calculate the actual values of the properties dependent on the available properties and environment.
 *
 * There are also methods combining this with {@link ReflectionUtils} to configure actual beans.
 *
 *
 *
 * @author Michiel Meeuwissen
 * @since 1.75
 */
@Slf4j
public class ConfigUtils {


    /**
     * Given a list of config file names, returns an array of resources representing it.
     * For every entry this returns two 'classpath' urls (the original one, and one which could be present in your own project) and a file in user.home/conf
     * @return An array twice the size of the input arguments
     */
    public static String[] getConfigFilesInHome(String... configFiles) {
        return Stream.concat(
            Arrays.stream(configFiles)
                .map(c -> Stream.of(
                    "classpath:/" + c,
                    "classpath:/override-" + c))
                .flatMap(s -> s)
            ,
            Arrays.stream(configFiles)
                .map(c -> System.getProperty("user.home") + File.separator + "conf" + File.separator + c)
        ).toArray(String[]::new);
    }

    /**
     *
     */
    public static Map<String, String> getPropertiesInHome(String... configFiles) {
        return getProperties(
            getConfigFilesInHome(configFiles)
        );

    }

    /**
     * Given a list of urls/files resolve it to map of key/value settings
     * Values also are 'subsituted' using {@link #substitute}
     */
    public static Map<String, String> getProperties(String... configFiles)  {
        return getProperties(new HashMap<>(), configFiles);
    }


    /**
     * Given a list of urls/files resolve it to map of key/value settings, and add it to the given map.
     *
     */
    public static Map<String, String> getProperties(Map<String, String> initial, String... configFiles) {
        return getProperties(initial, (s) -> s, configFiles);
    }


    /**
     * Given a list of urls/files resolve it to map of key/value settings, and add it to the given map.
     *
     * The keys of this map are not {@link String}s but object generated by the given keyGenerator.
     *
     * This allows for coalescing of equivalent keys.
     */
    public static <K> Map<K, String> getProperties(Map<K, String> initial, Function<String, K> keyGenerator, String... configFiles) {

        Map<String, String> subst = new HashMap<>();
        for (Map.Entry<K, String> entry : initial.entrySet()) {
            subst.put(String.valueOf(entry.getKey()), String.valueOf(entry.getValue()));
        }

        for (String configFile : configFiles) {
            Properties properties = new Properties();
            log.debug("Reading {}", configFile);
            if (configFile.startsWith("classpath:")) {
                InputStream in = ReflectionUtils.class.getResourceAsStream(configFile.substring("classpath:".length()));
                if (in != null) {
                    log.info("Reading properties from classpath {}", configFile);
                    try {
                        properties.load(in);
                    } catch (IOException ioe) {
                        log.error(ioe.getMessage());
                    }
                }
            } else {
                File file = new File(configFile);
                if (! file.exists()) {
                    log.debug("The file {} does  not exists", file);
                } else if (! file.canRead()) {
                    log.info("The file {} cannot be read", file);
                } else {
                    try {
                        log.info("Reading properties from {}", file);
                        properties.load(new FileInputStream(file));
                    } catch (IOException ioe) {
                        log.error(ioe.getMessage());
                    }
                }
            }
            for (Map.Entry<Object, Object> entry : properties.entrySet()) {
                initial.put(keyGenerator.apply(String.valueOf(entry.getKey())), String.valueOf(entry.getValue()));
                subst.put(String.valueOf(entry.getKey()), String.valueOf(entry.getValue()));
            }
        }



        substitute(initial, subst);
        return Collections.unmodifiableMap(initial);
    }



    /**
     * Creates a configured instance of given class.
     * @param env The OTAP environment to configure for (see {@link #filtered(Env, Map)}
     * @param clazz The class to create an instance for
     * @param config A map of configurations
     */
    public static <T> T configured(Env env, Class<T> clazz, Map<String, String> config) {
        Map<String, String> properties = filtered(env, config);
        return ReflectionUtils.configured(clazz, properties);
    }

    public static <T> T configured(Env env, T instance, Map<String, String> config) {
        Map<String, String> properties = filtered(env, config);
        return ReflectionUtils.configured(instance, properties);
    }

    public static <T> T configured(Env env, Class<T> clazz, String... configFiles) {
        return configured(env, clazz, getProperties(configFiles));
    }

    public static <T> T configured(Env env, T instance, String... configFiles) {
        Map<String, String> properties = filtered(env, getProperties(configFiles));
        return configured(env, instance, properties);
    }


    public static <T> T configured(Class<T> clazz, Map<String, String> props) {
        return configured(getEnv(props), clazz, props);
    }

    public static <T> T configured(T instance, Map<String, String> props) {
        return configured(getEnv(props), instance, props);
    }

    public static <T> T configured(Class<T> clazz, String... configFiles) {
        Map<String, String> props = getProperties(configFiles);
        return configured(clazz, props);
    }

    public static <T> T configured(T instance, String... configFiles) {
        Map<String, String> props = getProperties(configFiles);
        return configured(instance, props);
    }

    public static <T> T configuredInHome(Env env, Class<T> clazz, String... configFiles) {
        Map<String, String> props = getPropertiesInHome(configFiles);
        return configured(env, clazz, props);
    }

    public static <T> T configuredInHome(Env env, T instance, String... configFiles) {
        Map<String, String> props = getPropertiesInHome(configFiles);
        return configured(env, instance, props);
    }

    public static <T> T configuredInHome(Class<T> clazz, String... configFiles) {
        Map<String, String> props = getPropertiesInHome(configFiles);
        return configured(getEnv(props), clazz, props);
    }

    public static <T> T configuredInHome(T instance, String... configFiles) {
        Map<String, String> props = getPropertiesInHome(configFiles);
        return configured(getEnv(props), instance, props);
    }

    static <K> void substitute(Map<K, String> map, Map<String, String> substMap) {
        StringSubstitutor subst = new StringSubstitutor(substMap);

        for (Map.Entry<K, String> e : map.entrySet()) {
            String v = e.getValue();
            String replaced = subst.replace(v);
            if (! StringUtils.equals(v, replaced)) {
                e.setValue(replaced);
                log.debug("{}: {} -> {}", e.getKey(), v, replaced);
            }
        }
    }

    /**
     * Defaulting version of {@link #filtered(Env, String, Map)}, with no prefix.
     */
    public static Map<String, String> filtered(Env e, Map<String, String> properties) {
        return filtered(e, null, properties);
    }

    /**
     * Given a map of strings, and an {@link Env} create a new map of properties, filtered according to the following ideas.
     *
     * First of all, every propery in the source map may be postfixed with {@code .<env>}, this means that it represents the value for a certain environment. In the resulting map it shadows the value (if present) without this postfix.
     *
     * Every propery may also be prefixed with a prefix, which will be taken of. If the prefix does not match the prefix argument, the property will be filtered out the resulting map.
     *
     * E.g. a properties file like
     * <pre>
     *     systema.a.prod=x
     *     systema.a.test=y
     *     systema.a=z
     *     systema.b.dev=8
     *     systemb.b=7
     *     systemb.a=5
     *     systemb.foo=bar
     * </pre>
     *
     * Filtered with arguments `env = {@link Env#DEV}` and prefix 'systema' this becomes
     * <pre>
     *     a=z
     *     b=8
     * </pre>
     *
     * The idea is that the resulting map can be fed to {@link ReflectionUtils#configured(Object, Map)}
     *
     * @param env
     * @param prefix
     * @param properties
     * @return
     */
    public static Map<String, String> filtered(final Env env, String prefix, Map<String, String> properties) {
        log.debug("Filtering{} for {}", prefix == null ? "" : prefix + " ", env);
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
            System.getProperty("env",
                properties.getOrDefault("env", "test"))
                .toUpperCase()
        );
    }


}
