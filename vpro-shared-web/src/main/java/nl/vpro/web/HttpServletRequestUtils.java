package nl.vpro.web;

import jakarta.servlet.ServletRequest;
import jakarta.servlet.http.HttpServletRequest;

/**
 * @since 2.29
 */
public class HttpServletRequestUtils {

    public static final String HTTP = "http";
    public static final String HTTPS = "https";

    public static final String X_FORWARDED_PROTO = "X-Forwarded-Proto";
    public static final String X_FORWARDED_HOST = "X-Forwarded-Host";
    public static final String X_FORWARDED_PORT = "X-Forwarded-Port";

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
        String server = getServerName(req);
        int port = getServerPort(req);
        StringBuilder url = new StringBuilder(scheme).append("://").append(server);
        appendPortPostFixIfNeeded(url, scheme, port);
        return url;
    }

    public static String getScheme(ServletRequest req) {
        String scheme = (req instanceof HttpServletRequest httpServletRequest) ? httpServletRequest.getHeader(X_FORWARDED_PROTO) : null;
        if (scheme == null) {
            scheme = req.getScheme();
        }
        return scheme;
    }

    public static String getServerName(ServletRequest req) {
        String serverName = (req instanceof HttpServletRequest httpServletRequest) ? httpServletRequest.getHeader(X_FORWARDED_HOST) : null;

        if (serverName == null) {
            serverName = req.getServerName();
        }
        return serverName;
    }

    public static int getServerPort(ServletRequest request) {
        int port = request.getServerPort();
        String portHeader = (request instanceof HttpServletRequest httpServletRequest) ? httpServletRequest.getHeader(X_FORWARDED_PORT) : null;
        if (portHeader != null) {
            try {
                port = Integer.parseInt(portHeader);
            } catch (NumberFormatException e) {
                // Use default port if header is invalid
            }
        }
        return port;
    }



    public static void appendPortPostFixIfNeeded(StringBuilder builder, String scheme, int port) {
        if (port > 0) {
            switch (scheme) {
                case HTTP:
                    if (port == 80) return;
                    break;
                case HTTPS:
                    if (port == 443) return;
                    break;
            }
            builder.append(':').append(port);
        }

    }

    public static String getPortPostFixIfNeeded(HttpServletRequest request) {
        StringBuilder build = new StringBuilder();
        appendPortPostFixIfNeeded(build, getScheme(request), getServerPort(request));
        return build.toString();
    }

    /**
     * @since 5.7
     */
    public static  String getOriginalRequestURL(HttpServletRequest request) {

        String scheme = getScheme(request);
        String host = getServerName(request);
        int port = getServerPort(request);

        // Build URL with port only if non-standard
        StringBuilder url = new StringBuilder();
        url.append(scheme).append("://").append(host);
        appendPortPostFixIfNeeded(url, scheme, port);

        // Add path and query string
        url.append(request.getRequestURI());
        if (request.getQueryString() != null) {
            url.append("?").append(request.getQueryString());
        }

        return url.toString();
    }
}
