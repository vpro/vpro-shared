package nl.vpro.configuration;

import java.io.*;
import java.util.Iterator;
import java.util.Properties;

import org.apache.commons.configuration.*;
import org.apache.log4j.Logger;
import org.springframework.web.context.ServletContextAware;

import javax.servlet.ServletContext;

/**
 * TODO move to
 * @author Michiel Meeuwissen
 * @version $Id$
 */
public class NPOConfigurationBean extends AbstractConfiguration implements ServletContextAware {

    private static final Logger LOG = Logger.getLogger(NPOConfigurationBean.class);

    private final String name;
    private final Properties properties = new Properties();

    public NPOConfigurationBean(String name) {
        this.name = name;
    }


    @Override
    public void setServletContext(ServletContext servletContext) {
        File dir = new File(servletContext.getRealPath("/"));
        if (dir.exists()) {
            File parent = dir.getParentFile();
            File configDir = new File(parent, "config");
            File configFile = new File(configDir, name);
            if (configFile.exists()) {
                try {
                    properties.load(new FileInputStream(configFile));
                    LOG.info("Loaded from " + configFile + " " + properties.keySet());
                } catch (IOException e) {
                    LOG.error(e.getMessage(), e);
                }
            } else {
                LOG.info("File " + configFile + " not found");
            }
        } else {
            LOG.info("No " + dir);
        }
    }

    @Override
    protected void addPropertyDirect(String key, Object value) {
        properties.setProperty(key, (String) value);
    }

    @Override
    public Iterator getKeys() {
        return properties.keySet().iterator();
    }

    @Override
    public Object getProperty(String s) {
        return properties.getProperty(s);
    }

    @Override
    public boolean containsKey(String s) {
        return properties.containsKey(s);
    }

    @Override
    public boolean isEmpty() {
        return properties.isEmpty();
    }

}
