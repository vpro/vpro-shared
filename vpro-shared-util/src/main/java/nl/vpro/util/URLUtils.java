package nl.vpro.util;

import java.net.URI;

public class URLUtils {

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

}
