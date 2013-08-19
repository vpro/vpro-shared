package nl.vpro.web.filter;

import java.io.IOException;

import javax.servlet.*;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
public class ExpireHeadersFilter implements Filter {
    private static final Logger log = LoggerFactory.getLogger(ExpireHeadersFilter.class);

    private static final int DEFAULT_TTL = 300;

    private FilterConfig filterConfig = null;

    private boolean development = false;
    private int ttl = DEFAULT_TTL;
    private String cacheControl;


    @Override
    public void init(FilterConfig filterConfig) {
        this.filterConfig = filterConfig;

        if(System.getProperty("development") != null) {
            development = "true".equals(System.getProperty("development"));
        }

        if(log.isDebugEnabled()) {
            log.debug("Using ExpireHeadersFilter");
        }
        this.ttl = getTTL();

        this.cacheControl = "public, max-age=" + ttl + ", must-revalidate";
    }

    @Override
    public void doFilter(ServletRequest req, ServletResponse res, FilterChain chain) throws IOException, ServletException {
        HttpServletResponse response = (HttpServletResponse)res;
        if(!development) {
            // Set the expire header
            response.setDateHeader("Expires", System.currentTimeMillis() + ttl);
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

    private int getTTL() {
        int ttl = DEFAULT_TTL;
        String value = filterConfig.getInitParameter("ttl");
        if(value != null && value.length() > 0) {
            try {
                ttl = Integer.valueOf(value);
            } catch(NumberFormatException n) {
                ttl = DEFAULT_TTL;
            }
        }
        return ttl;
    }
}
