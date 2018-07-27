package nl.vpro.filter;

import java.io.IOException;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * Puts a few things related to authentication and accountability (remote address) on the Mapped Diagnostic Context of SLF4J
 * so that they can easily be added to the log.
 * @author Michiel Meeuwissen
 * @since 0.30
 */
public class  MDCFilter implements Filter {

    public static final String USER_NAME = "userName";
    public static final String ONBEHALFOF= "onBehalfOf";
    public static final String REQUEST = "request";
    public static final String REMOTE_ADDR= "remoteAddr";

    boolean clear = false;

    @Override
    public void init(FilterConfig filterConfig) {
        if (filterConfig.getInitParameter("clear") != null) {
            clear = Boolean.valueOf(filterConfig.getInitParameter("clear"));
        }

    }

    @Override
    public void doFilter(ServletRequest req, ServletResponse res, FilterChain chain) throws IOException, ServletException {

        HttpServletRequest request = (HttpServletRequest) req;
        HttpServletResponse response  = (HttpServletResponse) res;
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null) {
            MDC.put(USER_NAME, auth.getName());
        }
        String query = request.getQueryString();
        String path = request.getRequestURI().substring(request.getContextPath().length());
        MDC.put(REQUEST, request.getMethod() + " " + path + (StringUtils.isEmpty(query) ? "" : ("?" + query)));

        String ipAddress = request.getHeader("X-FORWARDED-FOR");
        if (ipAddress == null) {
            ipAddress = request.getRemoteAddr();
        }
        MDC.put(REMOTE_ADDR, ipAddress);
        try {
            chain.doFilter(req, res);
        } finally {
            // access logging...
            Logger logger = LoggerFactory.getLogger(MDCFilter.class.getName() + path.replace('/', '.'));
            logger.debug("{} {}", response.getStatus(), response.getContentType());
            if (clear) {
                MDC.clear();
            } else {
                MDC.remove(USER_NAME);
                MDC.remove(REQUEST);
                MDC.remove(REMOTE_ADDR);
            }
        }
    }

    @Override
    public void destroy() {


    }
}
