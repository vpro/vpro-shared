package nl.vpro.logging.mdc;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.MDC;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * @author Michiel Meeuwissen
 * @since 1.79
 */
public class MDCConstants {

    public static final String USER_NAME    = "userName";
    public static final String ON_BEHALF_OF = "onBehalfOf";
    public static final String REQUEST      = "request";
    public static final String REMOTE_ADDR  = "remoteAddr";
    public static final String USER_AGENT   = "userAgent";
    public static final String BODY         = "body";

    public static final String HEADERS      = "headers";
    public static final String USER_COUNT      = "userCount";


    public static void onBehalfOf(String user) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null) {
            MDC.put(USER_NAME, String.valueOf(authentication.getPrincipal()));
        }
        String currentUser = MDC.get(USER_NAME);
        MDC.put(ON_BEHALF_OF, StringUtils.isEmpty(currentUser) ? user  : (":" + user));

    }
}
