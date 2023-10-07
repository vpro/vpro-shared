/*
 * Copyright (C) 2016 All rights reserved
 * VPRO The Netherlands
 */
package nl.vpro.cors;

import java.io.IOException;
import java.util.*;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.apache.commons.lang3.StringUtils;

/**
 * @author rico
 * @since 0.47
 */
public class CorsFilter implements Filter {

    private static final List<String> METHODS = Collections.unmodifiableList(Arrays.asList("GET", "HEAD", "OPTIONS"));

    @Override
    public void init(FilterConfig filterConfig) {

    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        if (!(request instanceof HttpServletRequest) || !(response instanceof HttpServletResponse)) {
            throw new ServletException("CorsFilter just supports HTTP requests");
        }
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;
        String origin = httpRequest.getHeader(CorsHeaders.ORIGIN);
        String method = httpRequest.getMethod();
        if (StringUtils.isNotEmpty(origin) && METHODS.contains(method)) {
            httpResponse.addHeader(CorsHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, origin);
            httpResponse.addHeader(CorsHeaders.ACCESS_CONTROL_ALLOW_METHODS, CorsHeaders.ACCESS_CONTROL_ALLOW_READ_METHODS_VALUE);
            httpResponse.addHeader(CorsHeaders.ACCESS_CONTROL_ALLOW_HEADERS, CorsHeaders.ACCESS_CONTROL_ALLOW_HEADERS_VALUE);
            httpResponse.addHeader(CorsHeaders.ACCESS_CONTROL_ALLOW_CREDENTIALS, "true");
        }
        chain.doFilter(httpRequest, httpResponse);
    }

    @Override
    public void destroy() {

    }
}
