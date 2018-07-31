package nl.vpro.util;


import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.function.Consumer;
import java.util.regex.Pattern;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.config.PropertyPlaceholderConfigurer;
import org.springframework.core.io.Resource;
import org.springframework.util.PropertyPlaceholderHelper;

/**
 * An extension of {@link PropertyPlaceholderConfigurer} that can do:
 * - exposes the map of properties (for use in e.g. JSP).
 * - expose some properties as system properties
 * - log some things
 * -Using {@link #setRegisterAsSingletonStringRegexp(Pattern)} you can also register specified properties as beans (this is usefull when using {@link javax.inject.Named} in stead of {@link org.springframework.beans.factory.annotation.Value}
 *
 * @author Michiel Meeuwissen
 */
@Slf4j
public class PropertiesUtil extends PropertyPlaceholderConfigurer  {

    private Map<String, String> propertiesMap;

    private Map<String, String> logMap = new HashMap<>();

    private String[] systemProperties;

    private int systemPropertiesMode = SYSTEM_PROPERTIES_MODE_FALLBACK;

    @Getter
    @Setter
    private Pattern registerAsSingletonStringRegexp = Pattern.compile("^$");

    @Getter
    @Setter
    private List<Consumer<Map<String, String>>> afterProperties;

    @Override
    protected void processProperties(
        ConfigurableListableBeanFactory beanFactory,
        Properties props) throws BeansException {
        super.processProperties(beanFactory, props);
        initMap(props);
        initSystemProperties();
        Set<String> registered = new HashSet<>();
        for (Map.Entry<String, String> e : propertiesMap.entrySet()) {
            if (registerAsSingletonStringRegexp.matcher(e.getKey()).matches()) {
                try {
                    if (!beanFactory.containsBeanDefinition(e.getKey())) {
                        registered.add(e.getKey());
                        beanFactory.registerSingleton(e.getKey(), e.getValue());
                    } else {
                        log.info("Could not register {} as a singleton string (it is already {})", e.getKey(), beanFactory.getBeanDefinition(e.getKey()));
                    }
                } catch (Exception ex) {
                    log.error(ex.getMessage());
                }
            }
        }
        if (registered.size() > 0) {
            log.info("Registered {} singleton strings: {} ", registered.size(), registered);
        }

        if (logMap.isEmpty()) {
            logger.debug(String.valueOf(getMap()));
        } else {
            PropertyPlaceholderHelper helper = new PropertyPlaceholderHelper(
                placeholderPrefix, placeholderSuffix, valueSeparator, ignoreUnresolvablePlaceholders);
            for (Map.Entry<String, String> logEntry : logMap.entrySet()) {
                String log = String.format(helper.replacePlaceholders(logEntry.getValue(), props), getMap().get(logEntry.getKey()));
                logger.info(log);
            }
        }
        if (afterProperties != null) {
            for (Consumer<Map<String, String>> after : afterProperties) {
                after.accept(propertiesMap);
            }
        }



    }

    public Map<String, String> getMap() {
        return Collections.unmodifiableMap(propertiesMap);
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
    public void setLocations(Resource[] locations) {

        System.out.println("Configuring with");
        for (Resource location : locations) {
            try {
                if (location != null) {
                    File file = location.getFile();
                    System.out.println(location + " -> " + file + " (" + (file.canRead() ? "can be read" : "not readable") + ")");
                }
            } catch (IOException ioe) {
                System.out.println(location);
            }
        }
        super.setLocations(locations);
    }

    private void initMap(Properties props) {

        Properties p = new Properties();
        if (this.systemPropertiesMode != SYSTEM_PROPERTIES_MODE_NEVER) {
            p.putAll(System.getProperties());
        }
        p.putAll(props);

        PropertyPlaceholderHelper helper = new PropertyPlaceholderHelper(
            placeholderPrefix, placeholderSuffix, valueSeparator, ignoreUnresolvablePlaceholders);


        propertiesMap = new HashMap<>();
        for(Object key : p.keySet()) {
            String keyStr = key.toString();
            String value = p.getProperty(keyStr);
            if (value == null && p.containsKey(keyStr)) value = "";
            String v = helper.replacePlaceholders(value, p);
            propertiesMap.put(keyStr, v);
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
                        logger.warn("Can not override System property " + property + " because it already exists");
                    }
                } else {
                    logger.error("Property " + property + " not found, please check the property configuration");
                }
            }
        }
    }

    @Override
    public String toString() {
        return "PropertiesUtil " + (propertiesMap == null ? null : propertiesMap.keySet());
    }
}
