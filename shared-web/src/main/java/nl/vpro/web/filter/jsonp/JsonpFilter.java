/**
 * Copyright (C) 2010 All rights reserved
 * VPRO The Netherlands
 */
package nl.vpro.web.filter.jsonp;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * <p>Wraps a JSON response with padding and sets the response content type to application
 * /javascript. This filter is triggered when a callback parameter is present on the URL
 * containing the callback methodname. If the name for the callback parameter is to obvious,
 * you can supply an alternative value in this filters init params.
 */
public class JsonpFilter implements Filter {
    private static final String CALLBACK = "callback";

    private String callback;

    public void init(FilterConfig config) throws ServletException {
        String callback = config.getInitParameter("callback");
        if(callback != null && !callback.equals("")) {
            this.callback = callback;
        } else {
            this.callback = CALLBACK;
        }
    }

    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        if(isJsonpRequest(request)) {
            String callback = request.getParameter(this.callback);

            if(callback.equals("")) {
                callback = CALLBACK;
            }

            AcceptJsonRequest requestWrapper = new AcceptJsonRequest((HttpServletRequest)request);
            JsonResponse responseWrapper = new JsonResponse((HttpServletResponse)response, callback);

            chain.doFilter(requestWrapper, responseWrapper);

            responseWrapper.flush();
        } else {
            chain.doFilter(request, response);
        }
    }

    public void destroy() {
    }

    private boolean isJsonpRequest(ServletRequest request) {
        return request instanceof HttpServletRequest
                && ((HttpServletRequest)request).getParameterMap().containsKey(callback);
    }
}
