/**
 * Copyright (C) 2010 All rights reserved
 * VPRO The Netherlands
 */
package nl.vpro.web.filter.jsontemplate;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * Returns a JSON response containing a property with the requested resource as its content.
 * <p>
 * Default usage:
 * http://localhost:8080/vpro/imagegallery/0.0.1/imagegallery.js?jstemplate=data
 * where the jstemplate parameter holds the name of the returned JSON property.
 *
 */
public class JsonTemplateFilter implements Filter {
    private static final String PROPERTY = "jstemplate";

    private String property;

    public void init(FilterConfig config) throws ServletException {
        String template = config.getInitParameter(PROPERTY);
        if(template != null && !template.equals("")) {
            this.property = template;
        } else {
            this.property = PROPERTY;
        }
    }

    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        if(isJsonpRequest(request)) {
            String property = request.getParameter(this.property);

            RequestWrapper requestWrapper = new RequestWrapper((HttpServletRequest)request);
            ResponseWrapper responseWrapper = new ResponseWrapper((HttpServletResponse)response, property);

            try {
                chain.doFilter(requestWrapper, responseWrapper);
                responseWrapper.flush();
            } catch(Exception e) {
                response.getWriter().write("{ \"error\" : \"" + e.getMessage() + "\"}");
                return;
            }

        } else {
            chain.doFilter(request, response);
        }
    }

    public void destroy() {
    }

    private boolean isJsonpRequest(ServletRequest request) {
        return request instanceof HttpServletRequest
                && ((HttpServletRequest)request).getMethod().equals("GET")
                // GetParameterMap flushes the inputstream for 
                // application/x-www-form-urlencoded PUT requests
                && (request).getParameterMap().containsKey(property);
    }
}
