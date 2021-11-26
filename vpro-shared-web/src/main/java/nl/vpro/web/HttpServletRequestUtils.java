package nl.vpro.web;

import javax.servlet.ServletRequest;
import javax.servlet.http.HttpServletRequest;

/**
 * @since 2.29
 */
public class HttpServletRequestUtils {

    private HttpServletRequestUtils() {
        // utility class
    }

    public static StringBuilder getContextURL(HttpServletRequest req) {
        StringBuilder url = getBaseURL(req);
        url.append(req.getContextPath());
        return url;
    }

    public static StringBuilder getBaseURL(ServletRequest req) {
        String scheme = req.getScheme();
        String server = req.getServerName();
        int port = req.getServerPort();

        StringBuilder url = new StringBuilder(scheme).append("://").append(server);
        if (port > 0 && (("http".equalsIgnoreCase(scheme) && port != 80) ||
                ("https".equalsIgnoreCase(scheme) && port != 443))) {
            url.append(':').append(port);
        }

        return url;
    }
}
