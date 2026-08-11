package nl.vpro.monitoring.web;

import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.*;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.meeuw.functional.OptionalBoolean;
import org.springframework.http.HttpHeaders;

import nl.vpro.monitoring.config.MonitoringProperties;

import org.springframework.objenesis.SpringObjenesis;

/**
 * Just provides  {@link #authenticate(HttpServletRequest, HttpServletResponse, MonitoringProperties)}   for the /manage/ endpoints.
 * It is just called from within
 * @since 5.7
 */
@Slf4j
public class Authentication {

    public static final String REALM = "manager";

    private Authentication() {
        // no instances
    }

    /**
     * We used to do this via spring security, but that's all pretty cumbersome, and some applications (e.g. image frontend) don't even need spring security, and we ended up
     * adding all that, just for this one authentication on /manage/metrics. So we do it ourselves. This is all.
     */
    static OptionalBoolean basic(Set<MonitoringProperties.Method> left, HttpServletRequest request, HttpServletResponse response, MonitoringProperties properties) throws IOException {
        if (left.remove(MonitoringProperties.Method.BASIC)) {
            String auth = request.getHeader(HttpHeaders.AUTHORIZATION);
            if (auth == null || !auth.startsWith("Basic ")) {
                if (left.isEmpty()) {
                    response.setHeader(HttpHeaders.WWW_AUTHENTICATE, "Basic realm=\"%s\"".formatted(REALM));
                    response.sendError(HttpServletResponse.SC_UNAUTHORIZED);
                    return OptionalBoolean.FALSE;
                }  else {
                    return OptionalBoolean.EMPTY;
                }
            }
            String credentials = new String(Base64.getDecoder().decode(auth.substring(6))); // Remove "Basic "
            String[] values = credentials.split(":", 2);
            if (values.length == 2) {
                String username = values[0];
                String password = values[1];
                if (properties.getUser().equals(username) && password.equals(properties.getPassword())) {
                    return OptionalBoolean.TRUE;
                }
            }
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED);
            return OptionalBoolean.FALSE;
        } else {
            return OptionalBoolean.EMPTY;
        }

    }

    /**
     * Authenticates the service account of the (OpenShift/Kubernetes) deployment: an incoming bearer token is
     * accepted when it equals the pod's service account token, as read from
     * {@link MonitoringProperties#getServiceTokenFile()}.
     * <p>
     * Returns {@code null} when bearer authentication is not applicable to this request (no bearer token
     * presented, or no service account token configured/readable) so the caller can fall back to another
     * authentication method without any response being written.
     * <p>
     * Once bearer authentication is clearly the method being attempted (a bearer token was presented and a
     * service account token is available to check it against), this is definitive: it returns {@code true} or
     * {@code false} and, on failure, decorates the response with a {@code 401} and a {@code WWW-Authenticate: Bearer}
     * challenge itself. The caller must not fall back to another authentication method in that case.
     */
    static OptionalBoolean service(Set<MonitoringProperties.Method> left, HttpServletRequest request, HttpServletResponse response, MonitoringProperties properties) throws IOException {

        if (left.remove(MonitoringProperties.Method.BEARER)) {

            String tokenFile = properties.getServiceTokenFile();
            if (tokenFile == null || tokenFile.isEmpty()) {
                return OptionalBoolean.EMPTY;
            }
            Path path = Path.of(tokenFile);
            if (!Files.isReadable(path)) {
                log.debug("No service account token readable at {}; bearer authentication disabled", path);
                return OptionalBoolean.EMPTY;
            }
            final String expectedToken;
            try {
                expectedToken = Files.readString(path).trim();
            } catch (IOException e) {
                log.warn("Could not read service account token from {}: {}", path, e.getMessage());
                return OptionalBoolean.EMPTY;
            }
            if (expectedToken.isEmpty()) {
                return OptionalBoolean.EMPTY;
            }

            String auth = request.getHeader(HttpHeaders.AUTHORIZATION);
            if (auth == null) {
                if (left.isEmpty()) {
                    response.setHeader(HttpHeaders.WWW_AUTHENTICATE, "Bearer realm=\"manager\"");
                    response.sendError(HttpServletResponse.SC_UNAUTHORIZED);
                    return OptionalBoolean.FALSE;
                } else {
                    // No attempt at authentication at all; not our concern, let basic() decide.
                    return OptionalBoolean.EMPTY;
                }

            }
            if (!auth.regionMatches(true, 0, "Bearer ", 0, 7)) {
                if (left.isEmpty()) {
                    response.setHeader(HttpHeaders.WWW_AUTHENTICATE, "Bearer realm=\"manager\"");
                    response.sendError(HttpServletResponse.SC_UNAUTHORIZED);
                    return OptionalBoolean.FALSE;
                } else {
                    // Some other scheme (e.g. Basic) was attempted; not our concern either.
                    return OptionalBoolean.EMPTY;
                }
            }
            String token = auth.substring(7).trim();
            if (token.isEmpty()) {
                // Bearer scheme used, but no token supplied: this is a malformed request, not just an unauthenticated one.
                response.setHeader(HttpHeaders.WWW_AUTHENTICATE, "Bearer realm=\"manager\", error=\"invalid_request\"");
                response.sendError(HttpServletResponse.SC_BAD_REQUEST);
                return OptionalBoolean.FALSE;
            }
            // constant-time comparison to avoid leaking the token via timing.
            boolean matches = MessageDigest.isEqual(
                expectedToken.getBytes(StandardCharsets.UTF_8),
                token.getBytes(StandardCharsets.UTF_8));
            if (!matches) {
                response.setHeader(HttpHeaders.WWW_AUTHENTICATE, "Bearer realm=\"manager\", error=\"invalid_token\"");
                response.sendError(HttpServletResponse.SC_UNAUTHORIZED);
                return OptionalBoolean.FALSE;
            } else {
                return OptionalBoolean.TRUE;
            }
        } else {
            return OptionalBoolean.EMPTY;
        }
    }


    public static  boolean authenticate(
        HttpServletRequest request,
        HttpServletResponse response,
        MonitoringProperties properties
    ) throws IOException {
        Set<MonitoringProperties.Method> methods = new HashSet<>(properties.getAuthenticationMethods());
        if (methods.isEmpty()) {
            return true;
        }
        OptionalBoolean serviceAuth = service(methods, request, response, properties);
        if (serviceAuth.isPresent()) {
            return serviceAuth.getAsBoolean();
        }
        return basic(methods, request, response, properties).orElse(false);
    }
}
