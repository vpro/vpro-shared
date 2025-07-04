package nl.vpro.logging.filter;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.io.Serial;
import java.util.function.Function;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;


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
public class  MDCFilter extends HttpFilter {

    @Serial
    private static final long serialVersionUID = 4190489434067156597L;

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

    @Getter
    @Setter
    static Function<Authentication, String> principal =
        s -> SecurityContextHolder.getContext().getAuthentication().getPrincipal().toString();

    @Override
    public void init(FilterConfig filterConfig) {
        if (filterConfig.getInitParameter("clear") != null) {
            clear = Boolean.parseBoolean(filterConfig.getInitParameter("clear"));
        }
    }



    @Override
    public void doFilter(HttpServletRequest request, HttpServletResponse response, FilterChain chain) throws IOException, ServletException {

        final String path = request.getRequestURI().substring(request.getContextPath().length());
        final String logPostFix = path.replace('/', '.');
        try {
            try {
                Authentication auth = SecurityContextHolder.getContext().getAuthentication();
                if (auth != null) {
                    String name = principal.apply(auth);
                    MDC.put(USER_NAME, name);
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


            chain.doFilter(request, response);
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

}
