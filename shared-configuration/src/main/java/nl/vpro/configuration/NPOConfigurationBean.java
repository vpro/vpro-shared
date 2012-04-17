package nl.vpro.configuration;

import java.io.*;
import java.net.URI;
import java.net.URL;
import java.util.Iterator;
import java.util.Properties;

import org.apache.commons.configuration.*;
import org.apache.log4j.Logger;
import org.springframework.web.context.ServletContextAware;

import javax.servlet.ServletContext;
import org.springframework.core.io.*;

/**
 * @author Michiel Meeuwissen
 * @version $Id$
 */
public class NPOConfigurationBean extends AbstractConfiguration implements ServletContextAware, Resource {

    private static final Logger LOG = Logger.getLogger(NPOConfigurationBean.class);

    private final String name;
    private final Properties properties = new Properties();
    private ServletContext sx;
    private File file = null;
    public NPOConfigurationBean(String name) {
        this.name = name;
    }


    @Override
    public File getFile() {
        if (file == null) {
            File dir = new File(sx.getRealPath("/"));
            if (!dir.exists()) { // DRS fixed, dir will never be null ofcourse...
            	dir = File.listRoots()[0];
            }
            File parent = dir.getParentFile();
            if (parent == null) parent = dir;
            File configDir = new File(parent, "config");
            file = new File(configDir, name);
        }
        return file;
    }

    @Override
    public void setServletContext(ServletContext servletContext) {
        sx = servletContext;
        getFile();
        if (file.exists()) {
            try {
                properties.load(new FileInputStream(file));
                LOG.info("Loaded from " + file + " " + properties.keySet());
            } catch (IOException e) {
                LOG.error(e.getMessage(), e);
            }
        } else {
            LOG.info("File " + file+ " not found");
        }
    }

    @Override
    protected void addPropertyDirect(String key, Object value) {
        properties.setProperty(key, (String) value);
    }

    @Override
    public Iterator<Object> getKeys() {
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

    @Override
    public boolean exists() {
        return ! properties.isEmpty();
    }

    @Override
    public boolean isReadable() {
        return !properties.isEmpty();
    }

    @Override
    public boolean isOpen() {
        return false;
    }

    @Override
    public URL getURL() throws IOException {
        return getFile().toURI().toURL();
    }

    @Override
    public URI getURI() throws IOException {
        return getFile().toURI();
    }



    @Override
    public long lastModified() throws IOException {
        return getFile().lastModified();
    }

    @Override
    public Resource createRelative(String relativePath) throws IOException {
        return new FileSystemResource(new File(getFile(), relativePath));
    }

    @Override
    public String getFilename() {
        return getFile().getName();
    }

    @Override
    public String getDescription() {
        return getFilename();
    }

    @Override
    public InputStream getInputStream() throws IOException {
        return new FileInputStream(getFile());
    }


	@Override
	public long contentLength() throws IOException {
		File file = this.getFile();
		if (file == null) {
			return 0;
		}
		
		return file.length();
	}
}
