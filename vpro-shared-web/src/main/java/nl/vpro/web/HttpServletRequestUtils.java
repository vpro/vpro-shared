package nl.vpro.web;

import jakarta.servlet.ServletRequest;
import jakarta.servlet.http.HttpServletRequest;

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

    /**
     * @since 5.7
     */
    public static  String getOriginalRequestURL(HttpServletRequest request) {

        // Get scheme - check forwarded headers first
        String scheme = request.getHeader("X-Forwarded-Proto");
        if (scheme == null) {
            scheme = request.getScheme();
        }

        // Get host
        String host = request.getHeader("X-Forwarded-Host");
        if (host == null) {
            host = request.getServerName();
        }

        // Get port
        int port = request.getServerPort();
        String portHeader = request.getHeader("X-Forwarded-Port");
        if (portHeader != null) {
            try {
                port = Integer.parseInt(portHeader);
            } catch (NumberFormatException e) {
                // Use default port if header is invalid
            }
        }

        // Build URL with port only if non-standard
        StringBuilder url = new StringBuilder();
        url.append(scheme).append("://").append(host);
        if (!((scheme.equals("http") && port == 80) || (scheme.equals("https") && port == 443))) {
            url.append(":").append(port);
        }

        // Add path and query string
        url.append(request.getRequestURI());
        if (request.getQueryString() != null) {
            url.append("?").append(request.getQueryString());
        }

        return url.toString();
    }
}
