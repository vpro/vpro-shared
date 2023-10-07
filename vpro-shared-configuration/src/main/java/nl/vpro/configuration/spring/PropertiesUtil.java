package nl.vpro.configuration.spring;


import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.function.Consumer;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import jakarta.inject.Provider;

import org.checkerframework.checker.nullness.qual.NonNull;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.config.PropertyPlaceholderConfigurer;
import org.springframework.core.SpringProperties;
import org.springframework.core.env.AbstractEnvironment;
import org.springframework.core.io.Resource;
import org.springframework.expression.Expression;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.util.PropertyPlaceholderHelper;

/**
 * An extension of {@link PropertyPlaceholderConfigurer} that can do:
 * <ul>
 * <li>expose the {@link #getMap()} map} of properties (for use in e.g. JSP).</li>
 * <li>expose some properties as {@link #setExposeAsSystemProperty(String) system properties}</li>
 * <li>{@link #setLog log} some things</li>
 * <li>Using {@link #setRegisterAsSingletonStringRegexp(Pattern)} you can also register specified properties as beans (this is usefull when using {@link javax.inject.Named} in stead of {@link org.springframework.beans.factory.annotation.Value}</li>
 * <li>Have a {@link #setAfterProperties(List)}  call back}</li>
 *</ul>
 * @author Michiel Meeuwissen
 */
@Slf4j
public class PropertiesUtil extends PropertyPlaceholderConfigurer  {

    private Map<String, String> propertiesMap;

    private Map<String, String> logMap = new HashMap<>();

    private String[] systemProperties;

    private int systemPropertiesMode = SYSTEM_PROPERTIES_MODE_FALLBACK;

    private boolean searchSystemEnvironment = !SpringProperties.getFlag(AbstractEnvironment.IGNORE_GETENV_PROPERTY_NAME);

    /**
     * All properties for which the key matches this regexp will be registered as a singleton String spring bean
     */
    @Getter
    @Setter
    private Pattern registerAsSingletonStringRegexp = Pattern.compile("^$");


    /**
     * All properties for which the key matches this regexp will be registered as a singleton String bean, and the most basic
     * {@link #toObject(String) type interference} using the value will be attempted. E.g. 'true' will be registered as a {@link Boolean}.
     */
    @Getter
    @Setter
    private Pattern registerAsSingletonObjectRegexp = Pattern.compile("^$");


    /**
     * A list of consumers that will receive {@link #getMap()} as soon as it is available.
     */
    @Getter
    @Setter
    private List<Consumer<Map<String, String>>> afterProperties;

    @Override
    protected void processProperties(
        @NonNull ConfigurableListableBeanFactory beanFactory,
        @NonNull Properties props) throws BeansException {
        putSystemPropertiesIfNeeded(props);
        super.processProperties(beanFactory, props);
        initMap(props);
        initSystemProperties();
        final Set<String> registeredAsString = new HashSet<>();
        final Set<String> registeredAsObject = new HashSet<>();
        for (Map.Entry<String, String> e : propertiesMap.entrySet()) {
            if (registerAsSingletonStringRegexp.matcher(e.getKey()).matches()) {
                try {
                    if (!beanFactory.containsBeanDefinition(e.getKey())) {
                        registeredAsString.add(e.getKey());
                        String v = e.getValue();
                        beanFactory.registerSingleton(e.getKey(), v);
                    } else {
                        log.info("Could not register {} as a singleton string (it is already {})", e.getKey(), beanFactory.getBeanDefinition(e.getKey()));
                    }
                } catch (Exception ex) {
                    log.error(ex.getMessage());
                }
            }
            if (registerAsSingletonObjectRegexp.matcher(e.getKey()).matches()) {
                try {
                    if (!beanFactory.containsBeanDefinition(e.getKey())) {
                        registeredAsObject.add(e.getKey());
                        String v = e.getValue();
                        beanFactory.registerSingleton(e.getKey(), toObject(v));
                    } else {
                        log.info("Could not register {} as a singleton object (it is already {})", e.getKey(), beanFactory.getBeanDefinition(e.getKey()));
                    }
                } catch (Exception ex) {
                    log.error(ex.getMessage());
                }
            }
        }
        if (!registeredAsString.isEmpty()) {
            log.info("Registered {} singleton strings: {} ", registeredAsString.size(), registeredAsString);
        }
        if (!registeredAsObject.isEmpty()) {
            log.info("Registered {} singleton objects: {} ", registeredAsObject.size(), registeredAsObject.stream().map(v -> v.getClass().getSimpleName() + ":" + v).collect(Collectors.toList()));
        }

        if (logMap.isEmpty()) {
            log.debug("{}", getMap());
        } else {
            PropertyPlaceholderHelper helper = new PropertyPlaceholderHelper(
                placeholderPrefix, placeholderSuffix, valueSeparator, ignoreUnresolvablePlaceholders);

            ExpressionParser parser = new SpelExpressionParser();
            PropertyPlaceholderHelper propertyPlaceholderHelper = new PropertyPlaceholderHelper("#{", "}");


            for (Map.Entry<String, String> logEntry : logMap.entrySet()) {
                String value = helper.replacePlaceholders(logEntry.getValue(), props);
                value = propertyPlaceholderHelper.replacePlaceholders(value, placeholderName -> {
                    try {
                        Expression exp = parser.parseExpression(placeholderName);
                        return (String) exp.getValue();
                    } catch (org.springframework.expression.spel.SpelEvaluationException e) {
                        log.warn(e.getMessage());
                        return placeholderName;
                    }
                });
                log.debug("{}", value);
            }
        }
        if (afterProperties != null) {
            for (Consumer<Map<String, String>> after : afterProperties) {
                after.accept(propertiesMap);
            }
        }
    }

    protected static Object toObject(String v) {
        if ("true".equals(v) || "false".equals(v)) {
            return Boolean.valueOf(v);
        }
        try {
            return Integer.parseInt(v);
        } catch (NumberFormatException ignored) {
        }
        try {
            return Long.parseLong(v);
        } catch (NumberFormatException ignore) {
        }
        try {
            return Float.parseFloat(v);
        } catch (NumberFormatException ignored) {
        }
        try {
            return Double.parseDouble(v);
        } catch (NumberFormatException ignored) {
        }
        return v;
    }

    public Map<String, String> getMap() {
        if (propertiesMap == null) {
            log.warn("Properties map not yet initialized");
            return new HashMap<>();
        }
        return Collections.unmodifiableMap(propertiesMap);
    }

    @Override
    public void loadProperties(@NonNull Properties properties) throws IOException {
        super.loadProperties(properties);
    }

    /**
     * @since 3.5
     */
    public Provider<Map<String, String>> provideMap() {
        return this::getMap;
    }

    public void setExposeAsSystemProperty(String properties) {
        systemProperties = properties.split("\\s*,\\s*");
    }

    public void setLog(Map<String, String> map) {
        this.logMap = map;
    }

    @Override
    public void setSystemPropertiesMode(int systemPropertiesMode) {
        super.setSystemPropertiesMode(systemPropertiesMode);
        this.systemPropertiesMode = systemPropertiesMode;
    }



    @Override
    public void setLocations(Resource @NonNull [] locations) {
        // this may happen before log4j initialization
        //Logger jul = Logger.getLogger(PropertiesUtil.class.getName());

        //jul.log(Level.CONFIG, "Configuring with");
        System.out.println("Configuring with");

        for (Resource location : locations) {
            try {
                if (location != null) {
                    File file = location.getFile();
                    System.out.println(location + " -> " + file + " (" + (file.canRead() ? "can be read" : "not readable") + ")");

                    //jul.log(Level.CONFIG, location + " -> " + file + " (" + (file.canRead() ? "can be read" : "not readable") + ")");
                }
            } catch (IOException ioe) {
                System.out.println(location);
                //jul.warning(location.toString() + ":" + ioe.getMessage());
            }
        }
        super.setLocations(locations);
    }

    private int  putSystemPropertiesIfNeeded(Properties p) {
        if (systemPropertiesMode != SYSTEM_PROPERTIES_MODE_NEVER) {
            p.putAll(System.getProperties());
            if (searchSystemEnvironment) {
                p.putAll(System.getenv());
            }
            log.debug("Put {} system properties: {}", System.getProperties().size(), System.getProperties().keySet());
            return System.getProperties().size();
        }
        return 0;
    }


    @Override
    public void setSearchSystemEnvironment(boolean searchSystemEnvironment) {
        super.setSearchSystemEnvironment(searchSystemEnvironment);
		this.searchSystemEnvironment = searchSystemEnvironment;
	}
    private void initMap(Properties props) {

        Properties p = new Properties();
        putSystemPropertiesIfNeeded(p);
        p.putAll(props);

        PropertyPlaceholderHelper helper = new PropertyPlaceholderHelper(
            placeholderPrefix, placeholderSuffix, valueSeparator, ignoreUnresolvablePlaceholders);


        propertiesMap = new HashMap<>();
        ExpressionParser parser = new SpelExpressionParser();
        PropertyPlaceholderHelper propertyPlaceholderHelper = new PropertyPlaceholderHelper("#{", "}");
        for(Object key : p.keySet()) {
            String keyStr = key.toString();
            String value = p.getProperty(keyStr);
            if (value == null) {
                if (p.containsKey(keyStr)) {
                    value = "";
                } else {
                    propertiesMap.put(keyStr, null);
                    continue;
                }
            }
            String v = helper.replacePlaceholders(value, p);
            String elV = propertyPlaceholderHelper.replacePlaceholders(v, placeholderName -> {
                try {
                    Expression exp = parser.parseExpression(placeholderName);
                    return (String) exp.getValue();
                } catch (org.springframework.expression.spel.SpelEvaluationException e) {
                    log.debug(e.getMessage());
                    return placeholderName;
                }
            });
            Object prevValue = propertiesMap.put(keyStr, elV);
            if (prevValue != null) {
                log.debug("Replaced {}: {} -> {}", keyStr, prevValue, elV);
            }
        }

    }

    private void initSystemProperties() {
        if(systemProperties != null) {
            for(String property : systemProperties) {
                String value = propertiesMap.get(property);
                if(value != null) {
                    if(System.getProperty(property) == null || localOverride) {
                        System.setProperty(property, value);
                    } else {
                        log.warn("Can not override System property " + property + " because it already exists");
                    }
                } else {
                    log.error("Property " + property + " not found, please check the property configuration");
                }
            }
        }
    }

    @Override
    public String toString() {
        return "PropertiesUtil " + (propertiesMap == null ? null : propertiesMap.keySet());
    }
}
