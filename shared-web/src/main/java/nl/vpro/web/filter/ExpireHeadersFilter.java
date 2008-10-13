package nl.vpro.web.filter;

import java.io.IOException;
import java.util.Calendar;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;

/**
 * Make sure that all pages will expire in 5 minutes.
 *
 * Cache-Control: max-age=300
 * Expires: currenttime + 300 seconds.
 *
 * For more information about these headers I refer to:
 * http://www.w3.org/Protocols/rfc2616/rfc2616-sec14.html
 *
 * @author Rob Vermeulen (VPRO)
 */
public class ExpireHeadersFilter implements Filter {
    private static final Logger log = Logger.getLogger(ExpireHeadersFilter.class);

    public void doFilter(ServletRequest req, ServletResponse res, FilterChain chain) throws IOException, ServletException {
        HttpServletResponse response = (HttpServletResponse) res;

        // Set the expire header
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.MINUTE, 5);
        response.setDateHeader("Expires" , cal.getTimeInMillis());

        // Set the max-age header.
        response.setHeader("Cache-Control", "max-age=300");

        // log.error("Setting expire headers ");
        chain.doFilter(req, res);
    }

    public void init(FilterConfig filterConfig) {
        log.info("Starting ExpireHeadersFilter");
    }

    public void destroy() {
    }
}