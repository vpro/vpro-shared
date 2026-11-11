package nl.vpro.util;

import java.net.URI;
import java.net.URLEncoder;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import static java.nio.charset.StandardCharsets.UTF_8;

public class URLUtils {

    /**
     * Adds a query parameter to the given URL. Existing parameters (also ones with the same name) are kept, and a fragment stays at the end.
     * The name and value are URL-encoded (UTF-8, spaces as {@code %20}). If {@code value} is {@code null}, only the name is added (e.g. {@code ?flag}).
     * @return the url with the parameter appended, or {@code null} if {@code url} is {@code null}
     * @since 5.20
     */
    public static String addQueryParameter(@Nullable String url, @NonNull String name, String value) {
        if (url == null) {
            return null;
        }
        if (name == null || name.isEmpty()) {
            throw new IllegalArgumentException("Query parameter name must not be empty");
        }
        final int hash = url.indexOf('#');
        final String base = hash < 0 ? url : url.substring(0, hash);
        final String fragment = hash < 0 ? "" : url.substring(hash);

        final StringBuilder result = new StringBuilder(base);
        if (base.indexOf('?') < 0) {
            result.append('?');
        } else if (!base.endsWith("?") && !base.endsWith("&")) {
            result.append('&');
        }
        result.append(encodeQueryComponent(name));
        if (value != null) {
            result.append('=').append(encodeQueryComponent(value));
        }
        return result.append(fragment).toString();
    }

    private static String encodeQueryComponent(String s) {
        return URLEncoder.encode(s, UTF_8).replace("+", "%20");
    }

    public static String hidePassword(String url) {
        if (url == null) {
            return null;
        }
        try {
            URI uri = URI.create(url);
            String userInfo = uri.getUserInfo();
            if (userInfo == null || !userInfo.contains(":")) {
                return url;
            }
            String username = userInfo.substring(0, userInfo.indexOf(':'));
            String maskedUserInfo = username + ":*****";
            URI sanitized = new URI(
                uri.getScheme(),
                maskedUserInfo,
                uri.getHost(),
                uri.getPort(),
                uri.getPath(),
                uri.getQuery(),
                uri.getFragment()
            );
            return sanitized.toString();
        } catch (Exception e) {
            // on any parse error, return original URL unchanged
            return url;
        }
    }

    public static String addAuthentication(String url, String basicAuth) {
        if (url == null) {
            return null;
        }
        try {
            URI uri = URI.create(url);
            URI sanitized = new URI(
                uri.getScheme(),
                basicAuth,
                uri.getHost(),
                uri.getPort(),
                uri.getPath(),
                uri.getQuery(),
                uri.getFragment()
            );
            return sanitized.toString();
        } catch (Exception e) {
            // on any parse error, return original URL unchanged
            return url;
        }
    }


    }
