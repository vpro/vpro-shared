package nl.vpro.configuration;

import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URL;
import java.util.Iterator;
import java.util.Properties;

import javax.servlet.ServletContext;

import org.apache.commons.configuration2.AbstractConfiguration;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.web.context.ServletContextAware;

/**
 * At NPO a bit odd way to set up applications is used. This makes it aware of that.
 *
 * It can be configured with just config file name, and it will then resolve it automaticly in in the config directory of the
 * application server /e/as/<server>/conf
 *
 * This simplifies configuration and elimates the need for several settings.
 *
 * @author Michiel Meeuwissen
 */
@Slf4j
public class NPOConfigurationBean extends AbstractConfiguration implements ServletContextAware, Resource {

    private final String name;
    private final Properties properties = new Properties();
    private ServletContext sx;
    private File file = null;
    public NPOConfigurationBean(String name) {
        this.name = name;
    }


    /**
     * The implicetely determined configuration file
     */
    @Override
    public File getFile() {
        if (file == null) {
            File configDir = new NPO(sx).getConfigDirectory();
            file = new File(configDir, name);
            log.debug("Found configuration file {}", file);
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
                log.info("Loaded from " + file + " " + properties.keySet());
            } catch (IOException e) {
                log.error(e.getMessage(), e);
            }
        } else {
            log.info("File " + file + " not found");
        }
    }

    @Override
    protected void addPropertyDirect(String key, Object value) {
        properties.setProperty(key, (String) value);
    }

    @Override
    protected void clearPropertyDirect(String s) {
        properties.remove(s);
    }

    @Override
    public Iterator<String> getKeysInternal() {
        final Iterator<Object> i = properties.keySet().iterator();
        return new Iterator<String>() {
            @Override
            public boolean hasNext() {
                return i.hasNext();

            }

            @Override
            public String next() {
                return String.valueOf(i.next());
            }

            @Override
            public void remove() {
                i.remove();
            }
        };
    }


    @Override
    public Object getPropertyInternal(String s) {
        return properties.getProperty(s);
    }

    @Override
    public boolean containsKeyInternal(String s) {
        return properties.containsKey(s);
    }

    @Override
    public boolean isEmptyInternal() {
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
    public URI getURI() {
        return getFile().toURI();
    }



    @Override
    public long lastModified() {
        return getFile().lastModified();
    }

    @Override
    public Resource createRelative(String relativePath) {
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
	public long contentLength() {
		File file = this.getFile();
		if (file == null) {
			return 0;
		}

		return file.length();
	}

    @Override
    public String toString() {
        return getFilename();
    }
}
