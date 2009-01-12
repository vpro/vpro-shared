/*
 * Copyright (C) 2005/2006/2007 All rights reserved
 * VPRO The Netherlands
 * Creation date 9-jan-2007.
 */

package nl.vpro.configuration;

import java.io.File;

import org.apache.commons.configuration.BaseConfiguration;
import org.apache.commons.configuration.Configuration;
import org.apache.commons.configuration.ConfigurationUtils;
import org.apache.commons.configuration.PropertiesConfiguration;
import org.apache.commons.configuration.reloading.FileChangedReloadingStrategy;
import org.apache.commons.configuration.reloading.ReloadingStrategy;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.core.io.Resource;

/**
 * Factorybean to create a configuration of properties when the resource exists.
 * 
 * @author arne
 * @version $Id$
 */
public class PropertyConfigurationFactoryBean implements InitializingBean, FactoryBean {

    private static final Logger log = Logger.getLogger(PropertyConfigurationFactoryBean.class);

    private Configuration configuration;

    private Resource location;

    private boolean reload;

    /**
     * @param location the config location
     */
    public void setLocation(Resource location) {
        this.location = location;
    }

    /**
     * @param reload use reloading of properties?
     */
    public void setReload(boolean reload) {
        this.reload = reload;
    }

    /** {@inheritDoc} */
    public void afterPropertiesSet() throws Exception {
        if (location.exists()) {
            log.info(String.format("resource %s does exists.", location.getDescription()));
            // If it is a file, use reloading strategy.
            PropertiesConfiguration propconfig = new PropertiesConfiguration(location.getURL());
            if (reload) {
                File f = ConfigurationUtils.fileFromURL(location.getURL());
                if (f != null && f.exists()) {
                    log.info(String.format("resource %s will be configured with file reloading strategy.", location
                            .getDescription()));
                    ReloadingStrategy strategy = new FileChangedReloadingStrategy();
                    propconfig.setReloadingStrategy(strategy);
                }
            }
            configuration = propconfig;
        } else {
            log.info(String.format("resource %s does not exist, using base config.", location.getDescription()));
            this.configuration = new BaseConfiguration();
        }
    }

    /** {@inheritDoc} */
    public Object getObject() throws Exception {
        return configuration;
    }

    /** {@inheritDoc} */
    public Class getObjectType() {
        return Configuration.class;
    }

    /** {@inheritDoc} */
    public boolean isSingleton() {
        return true;
    }

    /**
     * @return Returns the location.
     */
    public Resource getLocation() {
        return location;
    }

    /**
     * @return Returns the reload.
     */
    public boolean isReload() {
        return reload;
    }
}
