package nl.vpro.logging.filter;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.util.function.Function;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.*;
import org.slf4j.event.Level;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import nl.vpro.logging.Slf4jHelper;

import static nl.vpro.logging.mdc.MDCConstants.*;

/**
 * Puts a few things related to authentication and accountability (remote address) on the Mapped Diagnostic Context of SLF4J
 * so that they can easily be added to the log.
 * @author Michiel Meeuwissen
 * @since 0.30
 */
@Slf4j
public class  MDCFilter implements Filter {

    boolean clear = false;

    /**
     * The log level to use for 'access' logging, as a function of the path.
     * <p>
     * Default this is {@link Level#DEBUG} unless it is a manage call, then {@link Level#TRACE}.
     *
     */
    @Getter
    @Setter
    Function<String, Level> accessLevel =
        s -> s.startsWith("/manage/") ? Level.TRACE : Level.DEBUG;

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
            final String query = request.getQueryString();
            MDC.put(REQUEST, request.getMethod() + " " + path + (StringUtils.isEmpty(query) ? "" : "?" + query));

            String ipAddress = request.getHeader("X-FORWARDED-FOR");
            if (ipAddress == null) {
                ipAddress = request.getRemoteAddr();
            }
            MDC.put(REMOTE_ADDR, ipAddress);

            final String userAgent = request.getHeader("User-Agent");
            if (userAgent != null) {
                MDC.put(USER_AGENT, userAgent);
            }


            chain.doFilter(req, res);
        } finally {
            // access logging...
            Slf4jHelper.log(afterLogger(logPostFix), accessLevel.apply(path), "{} {}", response.getStatus(), response.getContentType());
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
