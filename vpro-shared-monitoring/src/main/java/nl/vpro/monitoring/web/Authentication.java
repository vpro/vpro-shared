package nl.vpro.monitoring.web;

import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
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
@Slf4j
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

    /**
     * Authenticates the service account of the (OpenShift/Kubernetes) deployment: an incoming bearer token is
     * accepted when it equals the pod's service account token, as read from
     * {@link MonitoringProperties#getServiceTokenFile()}.
     * <p>
     * Unlike {@link #basic(HttpServletRequest, HttpServletResponse, MonitoringProperties) basic} this does <em>not</em>
     * send an error response when it fails, so the caller can fall back to another authentication method.
     */
    static boolean service(HttpServletRequest request, HttpServletResponse response, MonitoringProperties properties) {
        String auth = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (auth == null || !auth.regionMatches(true, 0, "Bearer ", 0, 7)) {
            return false;
        }
        String token = auth.substring(7).trim();

        String tokenFile = properties.getServiceTokenFile();
        if (tokenFile == null || tokenFile.isEmpty()) {
            return false;
        }
        Path path = Path.of(tokenFile);
        if (!Files.isReadable(path)) {
            log.debug("No service account token readable at {}; bearer authentication disabled", path);
            return false;
        }
        final String expectedToken;
        try {
            expectedToken = Files.readString(path).trim();
        } catch (IOException e) {
            log.warn("Could not read service account token from {}: {}", path, e.getMessage());
            return false;
        }
        if (expectedToken.isEmpty()) {
            return false;
        }
        // constant-time comparison to avoid leaking the token via timing.
        return MessageDigest.isEqual(
            expectedToken.getBytes(StandardCharsets.UTF_8),
            token.getBytes(StandardCharsets.UTF_8));
    }
}
