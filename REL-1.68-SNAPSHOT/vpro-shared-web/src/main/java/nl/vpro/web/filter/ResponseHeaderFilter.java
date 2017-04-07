package nl.vpro.web.filter;

import java.io.IOException;
import java.util.Enumeration;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Filter to set headers on all matching requests, the following example sets headers to avoid caching:
 * <p/>
 * <filter> <filter-name>PrivateResponseHeaderFilter</filter-name>
 * <filter-class>nl.vpro.dvt.communities.web.ResponseHeaderFilter</filter-class> <init-param>
 * <param-name>Cache-Control</param-name> <param-value>private,no-cache,no-store</param-value> </init-param>
 * <init-param> <param-name>Pragma</param-name> <param-value>no-cache</param-value> </init-param> </filter>
 *
 * @author Peter Maas <peter.maas@finalist.com>
 */
public class ResponseHeaderFilter implements Filter {
    private static final Logger log = LoggerFactory.getLogger(ResponseHeaderFilter.class);

    FilterConfig filterConfig;

    @SuppressWarnings("unchecked")
    public void doFilter(ServletRequest req, ServletResponse res, FilterChain chain) throws IOException, ServletException {
        HttpServletResponse response = (HttpServletResponse)res;
        // set the provided HTTP response parameters
        for(Enumeration e = filterConfig.getInitParameterNames(); e.hasMoreElements(); ) {
            String headerName = (String)e.nextElement();
            String headerValue = filterConfig.getInitParameter(headerName);
            if(log.isDebugEnabled()) {
                log.debug(String.format("setting header %s, value=%s", headerName, headerValue));
            }

            response.addHeader(headerName, headerValue);
        }
        // pass the request/response on

        HttpServletRequest request = (HttpServletRequest)req;
        // control setup obfuscates the url path, this parameter is used to retrieve the initial
        // request path.
        request.setAttribute("initialRequestURL", request.getRequestURL().toString());

        // Overrides the name of the character encoding used in the body of this request.
        // This method must be called prior to reading request parameters or reading input using getReader().
        chain.doFilter(request, response);
    }

    public void init(FilterConfig filterConfig) {
        log.info("starting");
        this.filterConfig = filterConfig;
    }

    public void destroy() {
    }
}