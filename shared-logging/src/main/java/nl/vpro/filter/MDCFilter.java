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
public class MDCFilter implements Filter {

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {


    }

    @Override
    public void doFilter(ServletRequest req, ServletResponse res, FilterChain chain) throws IOException, ServletException {

        HttpServletRequest request = (HttpServletRequest) req;
        HttpServletResponse response  = (HttpServletResponse) res;
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null) {
            MDC.put("userName", auth.getName());
        }
        String query = request.getQueryString();
        String path = request.getRequestURI().substring(request.getContextPath().length());
        MDC.put("request", request.getMethod() + " " + path + (StringUtils.isEmpty(query) ? "" : ("?" + query)));

        String ipAddress = request.getHeader("X-FORWARDED-FOR");
        if (ipAddress == null) {
            ipAddress = request.getRemoteAddr();
        }
        MDC.put("remoteAddr", ipAddress);
        try {
            chain.doFilter(req, res);
        } finally {
            // access logging...
            Logger logger = LoggerFactory.getLogger(MDCFilter.class.getName() + path.replace('/', '.'));
            logger.debug("{}", response.getStatus());
            MDC.remove("userName");
            MDC.remove("request");
            MDC.remove("remoteAddr");
        }
    }

    @Override
    public void destroy() {


    }
}
