package nl.vpro.web;

import javax.servlet.ServletRequest;
import javax.servlet.http.HttpServletRequest;

/**
 * @since 2.29
 */
public class HttpServletRequestUtils {

    public static final String HTTP = "http";
    public static final String HTTPS = "https";

    private HttpServletRequestUtils() {
        // utility class
    }

    public static StringBuilder getContextURL(HttpServletRequest req) {
        StringBuilder url = getBaseURL(req);
        url.append(req.getContextPath());
        return url;
    }

    public static StringBuilder getBaseURL(ServletRequest req) {
        String scheme = getScheme(req);
        String server = req.getServerName();
        int port = req.getServerPort();

        StringBuilder url = new StringBuilder(scheme).append("://").append(server);
        if (port > 0 && ((HTTP.equalsIgnoreCase(scheme) && port != 80) ||
                (HTTPS.equalsIgnoreCase(scheme) && port != 443))) {
            url.append(':').append(port);
        }

        return url;
    }

    public static String getScheme(ServletRequest req) {
        String scheme = (req instanceof HttpServletRequest) ? ((HttpServletRequest)req).getHeader("X-Forwarded-Proto") : null;
        if (scheme == null) {
            scheme = req.getScheme();
        }
        return scheme;
    }

    public static String getPortPostFixIfNeeded(HttpServletRequest req) {
        int port = req.getServerPort();
        String scheme = getScheme(req);
        switch(scheme) {
            case HTTP:
                if (port == 80) return "";
                break;
            case HTTPS:
                if (port == 443) return "";
                break;
        }
        return ":" + port;

    }
}
