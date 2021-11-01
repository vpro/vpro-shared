package nl.vpro.web.filter;

import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.time.Duration;

import javax.servlet.*;
import javax.servlet.http.HttpServletResponse;

import static nl.vpro.util.TimeUtils.parseDuration;

/**
 * Make sure that all pages will expire in 5 minutes.
 * <p/>
 * Cache-Control: max-age=300
 * Expires: currenttime + 300 seconds.
 * <p/>
 * For more information about these headers I refer to:
 * http://www.w3.org/Protocols/rfc2616/rfc2616-sec14.html
 *
 * @author Rob Vermeulen (VPRO)
 */
@Slf4j
public class ExpireHeadersFilter implements Filter {

    private static final Duration DEFAULT_TTL = Duration.ofMinutes(5);

    private FilterConfig filterConfig = null;

    private boolean development = false;
    private Duration ttl = DEFAULT_TTL;
    private String cacheControl;


    @Override
    public void init(FilterConfig filterConfig) {
        this.filterConfig = filterConfig;
        if(System.getProperty("development") != null) {
            development = "true".equals(System.getProperty("development"));
        }

        log.debug("Using ExpireHeadersFilter");
        this.ttl = getTTL();
        this.cacheControl = "public, max-age=" + (ttl.toMillis()  / 1000) + ", must-revalidate";
    }

    @Override
    public void doFilter(ServletRequest req, ServletResponse res, FilterChain chain) throws IOException, ServletException {
        HttpServletResponse response = (HttpServletResponse)res;
        if(!development) {
            // Set the expire header
            response.setDateHeader("Expires", System.currentTimeMillis() + ttl.toMillis());
            // Set the max-age header.
            response.setHeader("Cache-Control", "public, max-age=" + cacheControl);
        } else {
            response.setHeader("Cache-Control", "no-cache");
        }

        chain.doFilter(req, res);
    }

    @Override
    public void destroy() {
    }

    private Duration getTTL() {
        Duration ttl = DEFAULT_TTL;
        String value = filterConfig.getInitParameter("ttl");
        if(value != null && value.length() > 0) {
            ttl = parseDuration(value).orElse(DEFAULT_TTL);

        }
        return ttl;
    }
}
