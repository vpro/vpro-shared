/*
 * Copyright (C) 2010 All rights reserved
 * VPRO The Netherlands
 */
package nl.vpro.web.filter.jsonp;

import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * <p>Wraps a JSON response with padding and sets the response content type to application
 * /javascript. This filter is triggered when a callback parameter is present on the URL
 * containing the name for the callback method to use. If the name for the callback
 * parameter is to obvious, you can supply an alternative value in this filters init config.
 */
public class JsonpFilter implements Filter {
    private static final String CALLBACK = "callback";

    private static final Pattern restrictedChars = Pattern.compile("[^A-Za-z0-9_]");

    private String callback;

    @Override
    public void init(FilterConfig config) throws ServletException {
        String callback = config.getInitParameter("callback");
        if(callback != null && !callback.equals("")) {
            this.callback = callback;
        } else {
            this.callback = CALLBACK;
        }
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        if(isJsonpRequest(request)) {
            String callback = request.getParameter(this.callback);

            if(callback.equals("")) {
                callback = CALLBACK;
            }

            Matcher matcher = restrictedChars.matcher(callback);
            if(matcher.find()) {
                ((HttpServletResponse)response).sendError(HttpServletResponse.SC_SERVICE_UNAVAILABLE, "Unsupported callback: " + callback);
                return;
            }

            RequestWrapper requestWrapper = new RequestWrapper((HttpServletRequest)request);
            ResponseWrapper responseWrapper = new ResponseWrapper((HttpServletResponse)response, callback);

            chain.doFilter(requestWrapper, responseWrapper);

            responseWrapper.flush();
        } else {
            chain.doFilter(request, response);
        }
    }

    @Override
    public void destroy() {
    }

    private boolean isJsonpRequest(ServletRequest request) {
        return request instanceof HttpServletRequest
                && ((HttpServletRequest)request).getMethod().equals("GET")
                // GetParameterMap flushes the inputstream for
                // application/x-www-form-urlencoded PUT requests
                && request.getParameterMap().containsKey(callback);
    }
}
