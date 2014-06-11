package nl.vpro.web.servlet;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.Callable;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.json.JSONException;
import org.json.JSONWriter;

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

    private  static String ATTRIBUTE_NAME = ConfigurationServlet.class.getName() + ".configuration";

    protected String name;

    protected static enum Environment {
        PROD,
        TEST,
        ACC,
        DEV
    }

    protected Map<String, String> systemProps;

    public static Context getContext() throws NamingException {
        InitialContext context = new InitialContext();
        return (Context)context.lookup("java:comp/env");
    }

    public static Map<String, Object> getProperties(ServletContext context) throws IOException {
        try {
            return ((Callable<Map<String, Object>>)context.getAttribute(ATTRIBUTE_NAME)).call();
        } catch (IOException e) {
            throw e;
        } catch(Exception e){
            throw new RuntimeException(e);
        }
    }

    @Override
    public void init() throws ServletException {
        ServletConfig config = getServletConfig();
        String name = config.getInitParameter("name");
        if(name == null || name.length() == 0) {
            throw new IllegalArgumentException(String.format("Must provide a valid configuration name, got: %s", name));
        }
        this.name = name;

        config.getServletContext().setAttribute(ATTRIBUTE_NAME, new Callable<Map<String, Object>>() {
            @Override
            public Map<String, Object> call() throws IOException {
                return ConfigurationServlet.this.getProperties();

            }
        });
    }

    @Override
    protected long getLastModified(HttpServletRequest req) {
        long lastModified = -1;
        String env = getEnvironment().toString().toLowerCase();
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
        throws ServletException, IOException {
        resp.setContentType("application/json");
        getSystem(req);
        Map<String, Object> props = getProperties();

        if(getLastModified(req) == -1) {
            resp.setDateHeader("Expires", System.currentTimeMillis() + Integer.parseInt((String)props.get("MaxAge")) * 1000);
        } else {
            resp.setHeader("Cache-Control", "max-age=" + props.get("MaxAge") + ", must-revalidate, public");
        }


        JSONWriter w = new JSONWriter(resp.getWriter());

        try {
            String varName = req.getParameter("var");
            if(varName != null) {
                resp.getWriter().write("var " + varName + " = ");
            }
            w.object();

            for(Map.Entry<String, String> e : getSystem(req).entrySet()) {
                w.key(e.getKey()).value(e.getValue());
            }

            //w.key("version").value(getVersion());
            w.key("configuration").object();
            for(Map.Entry<String, Object> prop : props.entrySet()) {
                w.key(prop.getKey()).value(prop.getValue());
            }
            w.endObject();
            w.endObject();
            if(varName != null) {
                resp.getWriter().write(";");
            }
        } catch(JSONException e) {
            throw new ServletException(e);
        }

    }


    protected void merge(InputStream is, Map<String, Object> map) throws IOException {
        if(is == null) {
            return;
        }
        for(Map.Entry<String, String> e : getProperties(is).entrySet()) {
            String value = e.getValue();
            for(Map.Entry<String, String> sp : getSystem(null).entrySet()) {
                value = value.replaceAll("\\$\\{" + sp.getKey() + "\\}", sp.getValue());
            }
            map.put(e.getKey(), value);
        }
    }

    protected void merge(File file, Map<String, Object> map) throws IOException {
        if(file.canRead()) {
            merge(new FileInputStream(file), map);
        }
    }
    protected void mergeServletContextResource(String path, Map<String, Object> map) throws IOException {
        String file = getServletContext().getRealPath(path);
        if (file != null) {
            merge(new File(file), map);
        }
    }

    protected Map<String, String> getSystem(HttpServletRequest req) throws IOException {
        if(systemProps == null) {

            Map<String, String> result = new LinkedHashMap<>();
            result.put("env", getEnvironment().toString());

            if (req != null) {
                int port = req.getServerPort();
                result.put("thisServer", req.getScheme() + "://" + req.getServerName() + (port == 80 ? "" : ":" + port) + req.getContextPath());
            }


            URL u = getServletContext().getResource("/version.properties");


            if(u != null) {
                result.putAll(getProperties(u.openStream()));
            }
            if (req != null) {
                systemProps = result;
            }
            return result;
        }
        return systemProps;
    }

    protected Environment getEnvironment() {
        String en = System.getProperty("vpro." + name + ".env");
        if(en != null) {
            return Environment.valueOf(en.toUpperCase());
        }
        try {
            Context env = getContext();
            String lookupName = env.composeName("env", "vpro/" + name);
            Object value = env.lookup(lookupName);
            if(value instanceof String) {
                return Environment.valueOf(((String)value).toUpperCase());
            }
        } catch(NamingException e) {
            //

        }
        return Environment.PROD;
    }


    protected Map<String, Object> getProperties() throws IOException {
        Map<String, Object> res = new LinkedHashMap<>();
        String env = getEnvironment().toString().toLowerCase();
        merge(getClass().getResourceAsStream("/" + name + ".properties"), res);
        merge(getClass().getResourceAsStream("/" + name + "." + env + ".properties"), res);

        mergeServletContextResource("/WEB-INF/classes/" + name + ".properties", res);
        mergeServletContextResource("/WEB-INF/classes/" + name + "." + env + ".properties", res);
        String home = System.getProperty("user.home");
        merge(new File(home +
            File.separator + "conf" + File.separator + name + ".properties"), res);
        merge(new File(home +
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
                public Object put(Object key, Object value) {
                    keys.put((String)key, (String)value);
                    return super.put(key, value);
                }
            };
            props.load(is);
            is.close();
        }

        return keys;

    }
}


