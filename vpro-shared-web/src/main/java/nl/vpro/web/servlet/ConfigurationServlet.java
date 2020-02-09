package nl.vpro.web.servlet;

import java.io.*;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.function.Function;

import javax.naming.*;
import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.servlet.http.*;

import org.checkerframework.checker.nullness.qual.NonNull;

import com.fasterxml.jackson.core.*;


/**
 * Serves a 'configuration.js' for javascript clients.
 * Expects an init param "name" with that it determines the 'environment' via
 * <ul>
 * <li>Either it is the system property vpro.[name].env</li>
 * <li>If that is missing it is the application context variable vpro/[name]/env</li>
 * <li>If that is missing too, it is {@link Environment#PROD}</li>
 * </ul>
 * Then it loads properties from all of:
 * <ul>
 * <li>The resource [name].properties</li>
 * <li>The resource [name].[env];properties (if exists)</li>
 * <li>The file /WEB-INF/classes/[name].properties (if exists)</li>
 * <li>The file /WEB-INF/classes/[name].[env];properties (if exists)</li>
 * <li>The file ${user.home}/conf/[name].properties (if exists)</li>
 * <li>The file ${user.home}/conf/[name].[env];properties (if exists)</li>
 * </ul>
 * while it overrides already defined ones.
 * <p/>
 * The result is then, together with some other properties returned in a javascript structure.
 *
 * @author Michiel Meeuwissen
 * @since 0.3
 */
public class ConfigurationServlet extends HttpServlet {

    private static final String ATTRIBUTE_NAME = ConfigurationServlet.class.getName() + ".configuration";


    protected enum Environment {
        PROD,
        TEST,
        ACC,
        DEV
    }

    protected final static Map<String, Map<String, String>> SYSTEM_PROP_MAP = new HashMap<>();

    public static Context getContext() throws NamingException {
        InitialContext context = new InitialContext();
        return (Context)context.lookup("java:comp/env");
    }

    public static Map<String, Object> getProperties(ServletContext context, HttpServletRequest request) {
        return ((Function<HttpServletRequest, Map<String, Object>>)context.getAttribute(ATTRIBUTE_NAME)).apply(request);
    }

    @Override
    public void init() {
        ServletConfig config = getServletConfig();
        String name = config.getInitParameter("name");
        if(name == null || name.length() == 0) {
            throw new IllegalArgumentException(String.format("Must provide a valid configuration name, got: %s", name));
        }


        config.getServletContext().setAttribute(ATTRIBUTE_NAME, (Function<HttpServletRequest, Map<String, Object>>) req -> {
            try {
                return ConfigurationServlet.this.getProperties(req);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }

        });
    }

    @Override
    protected long getLastModified(HttpServletRequest req) {
        long lastModified = -1;
        String env = getEnvironment().toString().toLowerCase();
        String name = getName();
        for(String s : new String[]{
            getServletContext().getRealPath("/WEB-INF/classes/" + name + ".properties"),
            getServletContext().getRealPath("/WEB-INF/classes/" + name + "." + env + ".properties"),
            System.getProperty("user.home") + File.separator + "conf" + File.separator + name + ".properties",
            System.getProperty("user.home") + File.separator + "conf" + File.separator + name + "." + env + ".properties"}) {
            if(s != null) {
                File f = new File(s);
                if(f.canRead() && f.lastModified() > lastModified) {
                    lastModified = f.lastModified();
                }
            }
        }
        return lastModified;
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
        throws  IOException {
        resp.setContentType("application/json");
        Map<String, Object> props = getProperties(req);

        if(getLastModified(req) == -1) {
            resp.setDateHeader("Expires", System.currentTimeMillis() + Integer.parseInt((String)props.get("MaxAge")) * 1000);
        } else {
            resp.setHeader("Cache-Control", "max-age=" + props.get("MaxAge") + ", must-revalidate, public");
        }
        write(req.getParameter("var"), resp.getOutputStream(), getSystem(req), getProperties(req));
    }

    protected void write(
            String varName,
            @NonNull OutputStream outputStream,
            @NonNull Map<String, String> system,
            @NonNull Map<String, Object> props

    ) throws IOException {
        JsonFactory jfactory = new JsonFactory();
        try(JsonGenerator w  = jfactory.createGenerator(outputStream, JsonEncoding.UTF8)) {

            if (varName != null) {
                outputStream.write(("var " + varName + " = ").getBytes(StandardCharsets.UTF_8));
            }
            outputStream.flush();
            w.writeStartObject();

            for (Map.Entry<String, String> e : system.entrySet()) {
                w.writeStringField(e.getKey(), e.getValue());
            }

            //w.key("version").value(getVersion());
            w.writeFieldName("configuration");
            w.writeStartObject();
            for (Map.Entry<String, Object> prop : props.entrySet()) {
                w.writeObjectField(prop.getKey(), prop.getValue());
            }
            w.writeEndObject();
            w.writeEndObject();
            w.flush();
            if (varName != null) {
                outputStream.write(';');
            }
        }

    }


    protected void merge(HttpServletRequest req, InputStream is, Map<String, Object> map) throws IOException {
        if(is == null) {
            return;
        }
        for(Map.Entry<String, String> e : getProperties(is).entrySet()) {
            String value = e.getValue();
            for(Map.Entry<String, String> sp : getSystem(req).entrySet()) {
                value = value.replaceAll("\\$\\{" + sp.getKey() + "}", sp.getValue());
            }
            map.put(e.getKey(), value);
        }
    }

    protected void merge(HttpServletRequest req, File file, Map<String, Object> map) throws IOException {
        if(file.canRead()) {
            merge(req, new FileInputStream(file), map);
        }
    }
    protected void mergeServletContextResource(HttpServletRequest req, String path, Map<String, Object> map) throws IOException {
        String file = getServletContext().getRealPath(path);
        if (file != null) {
            merge(req, new File(file), map);
        }
    }

    private String getRequestKey(HttpServletRequest req) {
        String scheme = req != null ? req.getHeader("X-Forwarded-Proto") : null;
        if (scheme == null && req != null) {
            scheme = req.getScheme();
        }
        return req == null ? "NULL" : (scheme + "://" + req.getServerName() + req.getServerPort()  +req.getContextPath());
    }
    protected Map<String, String> getSystem(HttpServletRequest req) throws IOException {
        String key = getRequestKey(req);
        Map<String, String> systemProps = SYSTEM_PROP_MAP.get(key);
        if(systemProps == null) {
            systemProps = new LinkedHashMap<>();
            systemProps.put("env", getEnvironment().toString());
            if(req != null) {
                int port = req.getServerPort();
                String scheme = req.getHeader("X-Forwarded-Proto");
                if (scheme == null) {
                    scheme = req.getScheme();
                }
                systemProps.put("thisServer", scheme + "://" + req.getServerName() + (port == 80 ? "" : ":" + port) + req.getContextPath());
            }


            URL u = getServletContext().getResource("/version.properties");


            if(u != null) {
                systemProps.putAll(getProperties(u.openStream()));
            }
            SYSTEM_PROP_MAP.put(key, systemProps);
        }
        return systemProps;
    }

    protected Environment getEnvironment() {
        String en = System.getProperty("vpro." + getName() + ".env");
        if(en != null) {
            return Environment.valueOf(en.toUpperCase());
        }
        try {
            Context env = getContext();
            String lookupName = env.composeName("env", "vpro/" + getName());
            Object value = env.lookup(lookupName);
            if(value instanceof String) {
                return Environment.valueOf(((String)value).toUpperCase());
            }
        } catch(NamingException e) {
            //

        }
        return Environment.PROD;
    }


    protected Map<String, Object> getProperties(HttpServletRequest req) throws IOException {
        Map<String, Object> res = new LinkedHashMap<>();
        String env = getEnvironment().toString().toLowerCase();
        String name = getName();
        merge(req, getClass().getResourceAsStream("/" + name + ".properties"), res);
        merge(req, getClass().getResourceAsStream("/" + name + "." + env + ".properties"), res);

        mergeServletContextResource(req, "/WEB-INF/classes/" + name + ".properties", res);
        mergeServletContextResource(req, "/WEB-INF/classes/" + name + "." + env + ".properties", res);
        String home = System.getProperty("user.home");
        merge(req, new File(home +
            File.separator + "conf" + File.separator + name + ".properties"), res);
        merge(req, new File(home +
            File.separator + "conf" + File.separator + name + "." + env + ".properties"), res);
        return res;
    }

    /**
     * Reads in properties. It uses the parser of {@link Properties}, but the returned Map has predictable iteration order.
     *
     * @param is InputStream to read as properties file
     */
    static Map<String, String> getProperties(InputStream is) throws IOException {

        final Map<String, String> keys = new LinkedHashMap<>();
        if(is != null) {
            Properties props = new Properties() {
                @Override
                public synchronized Object put(Object key, Object value) {
                    keys.put((String)key, (String)value);
                    return super.put(key, value);
                }
            };
            props.load(is);
            is.close();
        }

        return keys;

    }

    protected String getName() {
        return getInitParameter("name");
    }
}


