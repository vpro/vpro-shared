package nl.vpro.filter;

import lombok.extern.slf4j.Slf4j;

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

import nl.vpro.mdc.MDCConstants;

/**
 * Puts a few things related to authentication and accountability (remote address) on the Mapped Diagnostic Context of SLF4J
 * so that they can easily be added to the log.
 * @author Michiel Meeuwissen
 * @since 0.30
 */
@Slf4j
public class  MDCFilter implements Filter {

    @Deprecated
    public static final String USER_NAME   = MDCConstants.USER_NAME;
    @Deprecated
    public static final String REQUEST     = MDCConstants.REQUEST;
    @Deprecated
    public static final String REMOTE_ADDR = MDCConstants.REMOTE_ADDR;

    boolean clear = false;

    @Override
    public void init(FilterConfig filterConfig) {
        if (filterConfig.getInitParameter("clear") != null) {
            clear = Boolean.parseBoolean(filterConfig.getInitParameter("clear"));
        }

    }

    @Override
    public void doFilter(ServletRequest req, ServletResponse res, FilterChain chain) throws IOException, ServletException {

        HttpServletRequest request = (HttpServletRequest) req;
        HttpServletResponse response  = (HttpServletResponse) res;
        String path = request.getRequestURI().substring(request.getContextPath().length());
        try {
            try {
                Authentication auth = SecurityContextHolder.getContext().getAuthentication();
                if (auth != null) {
                    MDC.put(MDCConstants.USER_NAME, auth.getName());
            }
            } catch (Exception e) {
                log.debug(e.getMessage());
            }
            String query = request.getQueryString();
            MDC.put(MDCConstants.REQUEST, request.getMethod() + " " + path + (StringUtils.isEmpty(query) ? "" : ("?" + query)));

            String ipAddress = request.getHeader("X-FORWARDED-FOR");
            if (ipAddress == null) {
                ipAddress = request.getRemoteAddr();
            }
            MDC.put(MDCConstants.REMOTE_ADDR, ipAddress);

            chain.doFilter(req, res);
        } finally {
            // access logging...
            Logger logger = LoggerFactory.getLogger(MDCFilter.class.getName() + path.replace('/', '.'));
            logger.debug("{} {}", response.getStatus(), response.getContentType());
            if (clear) {
                MDC.clear();
            } else {
                MDC.remove(MDCConstants.USER_NAME);
                MDC.remove(MDCConstants.REQUEST);
                MDC.remove(MDCConstants.REMOTE_ADDR);
            }
        }
    }

    @Override
    public void destroy() {


    }
}
