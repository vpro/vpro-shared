package nl.vpro.jmx;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import nl.vpro.util.ReflectionUtils;

/**
 * @author Michiel Meeuwissen
 * @since ...
 */
@Configuration
public class MBeansConfiguration {
    @Bean
    public BasalMBean getBasalMBean() {
        return new BasalMBean();
    }

    @Bean
    public Object getGroovyMBean() {
        if (ReflectionUtils.hasClass("groovy.lang.GroovyObject")) {
            return new GroovyMBean();
        } else {
            return null;
        }
    }
}
