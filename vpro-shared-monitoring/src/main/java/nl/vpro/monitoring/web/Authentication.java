package nl.vpro.monitoring.web;

import java.io.IOException;
import java.util.Base64;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.http.HttpHeaders;

import nl.vpro.monitoring.config.MonitoringProperties;

/**
 * Just provides  {@link #basic(HttpServletRequest, HttpServletResponse, MonitoringProperties) basic authentication} for the /manage/ endpoints.
 * It is just called from within
 * @since 5.7
 */
public class Authentication {

    private Authentication() {
        // no instances
    }

    /**
     * We used to do this via spring security, but that's all pretty cumbersome, and some applications (e.g. image frontend) don't even need spring security, and we ended up
     * adding all that, just for this one authentication on /manage/metrics. So we do it ourselves. This is all.
     */
    static boolean basic(HttpServletRequest request, HttpServletResponse response, MonitoringProperties properties) throws IOException {
        String auth = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (auth == null || !auth.startsWith("Basic ")) {
            response.setHeader(HttpHeaders.WWW_AUTHENTICATE, "Basic realm=\"manager\"");
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED);
            return false;
        }
        String credentials = new String(Base64.getDecoder().decode(auth.substring(6))); // Remove "Basic "
        String[] values = credentials.split(":", 2);
        if (values.length == 2) {
            String username = values[0];
            String password = values[1];
            if (properties.getUser().equals(username) && password.equals(properties.getPassword())) {
                return true;
            }
        }
        response.sendError(HttpServletResponse.SC_UNAUTHORIZED);
        return false;

    }
}
