package nl.vpro.configuration;

import org.apache.commons.configuration.CompositeConfiguration;
import org.apache.commons.configuration.Configuration;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.InitializingBean;

/**
 * FactoryBean which wraps a Commons CompositeConfiguration object for usage with PropertiesLoaderSupport. This allows
 * the configuration object to behave like a normal java.util.Properties object which can be passed on to
 * setProperties() method allowing PropertyOverrideConfigurer and PropertyPlaceholderConfigurer to take advantage of
 * Commons Configuration. <p/> Internally a CompositeConfiguration object is used for merging multiple Configuration
 * objects.
 *
 * @see java.util.Properties
 * @see org.springframework.core.io.support.PropertiesLoaderSupport
 * @author Costin Leau
 * 
 */
public class CommonsConfigurationFactoryBean implements InitializingBean, FactoryBean {

    private CompositeConfiguration configuration;

    private Configuration[] configurations;

    /**
     * {@inheritDoc}
     */
    public Object getObject() throws Exception {
        return (configuration != null) ? configuration : null;
    }

    /**
     * {@inheritDoc}
     */
    public Class getObjectType() {
        return Configuration.class;
    }

    /**
     * {@inheritDoc}
     */
    public boolean isSingleton() {
        return true;
    }

    /**
     * @return Returns the configurations.
     */
    public Configuration[] getConfigurations() {
        return configurations;
    }

    /**
     * Set the configurations objects which will be used as properties.
     *
     * @param configurations the configurations to set.
     */
    public void setConfigurations(Configuration[] configurations) {
        this.configurations = configurations;
    }

    /**
     * {@inheritDoc}
     */
    public void afterPropertiesSet() throws Exception {
        if (configurations == null || configurations.length == 0) {
            throw new IllegalArgumentException("at least one configuration");
        }

        configuration = new CompositeConfiguration();

        for (int i = 0; i < configurations.length; i++) {
            configuration.addConfiguration(configurations[i]);
        }
    }

}
