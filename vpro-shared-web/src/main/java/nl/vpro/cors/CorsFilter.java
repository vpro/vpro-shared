/*
 * Copyright (C) 2016 All rights reserved
 * VPRO The Netherlands
 */
package nl.vpro.cors;

import org.apache.commons.lang3.StringUtils;

import java.io.IOException;
import java.io.Serial;
import java.util.Set;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.*;

/**
 * @author rico
 * @since 0.47
 */
public class CorsFilter extends HttpFilter {

    private static final Set<String> METHODS = Set.of("GET", "HEAD", "OPTIONS");

    @Serial
    private static final long serialVersionUID = -6162772963452992019L;

    @Override
    public void doFilter(HttpServletRequest httpRequest, HttpServletResponse httpResponse, FilterChain chain) throws IOException, ServletException {
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

}
