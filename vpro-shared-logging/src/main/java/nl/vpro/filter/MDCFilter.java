package nl.vpro.filter;

import lombok.extern.slf4j.Slf4j;

import java.io.IOException;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.*;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import static nl.vpro.mdc.MDCConstants.*;

/**
 * Puts a few things related to authentication and accountability (remote address) on the Mapped Diagnostic Context of SLF4J
 * so that they can easily be added to the log.
 * @author Michiel Meeuwissen
 * @since 0.30
 */
@Slf4j
public class  MDCFilter implements Filter {

    boolean clear = false;

    @Override
    public void init(FilterConfig filterConfig) {
        if (filterConfig.getInitParameter("clear") != null) {
            clear = Boolean.parseBoolean(filterConfig.getInitParameter("clear"));
        }
    }

    @Override
    public void doFilter(ServletRequest req, ServletResponse res, FilterChain chain) throws IOException, ServletException {

        final HttpServletRequest request = (HttpServletRequest) req;
        final HttpServletResponse response  = (HttpServletResponse) res;
        final String path = request.getRequestURI().substring(request.getContextPath().length());
        final String logPostFix = path.replace('/', '.');
        try {
            try {
                Authentication auth = SecurityContextHolder.getContext().getAuthentication();
                if (auth != null) {
                    MDC.put(USER_NAME, auth.getName());
                }
            } catch (Exception e) {
                log.debug(e.getMessage());
            }
            String query = request.getQueryString();
            MDC.put(REQUEST, request.getMethod() + " " + path + (StringUtils.isEmpty(query) ? "" : ("?" + query)));

            String ipAddress = request.getHeader("X-FORWARDED-FOR");
            if (ipAddress == null) {
                ipAddress = request.getRemoteAddr();
            }
            MDC.put(REMOTE_ADDR, ipAddress);

            chain.doFilter(req, res);
        } finally {
            // access logging...
            afterLogger(logPostFix).debug("{} {}", response.getStatus(), response.getContentType());
            if (clear) {
                MDC.clear();
            } else {
                MDC.remove(USER_NAME);
                MDC.remove(REQUEST);
                MDC.remove(REMOTE_ADDR);
            }
        }
    }


    private Logger afterLogger(String postfix) {
        return LoggerFactory.getLogger(MDCFilter.class.getName() + "." + postfix);
    }

    @Override
    public void destroy() {


    }
}
