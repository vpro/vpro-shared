/**
 * Copyright (C) 2010 All rights reserved
 * VPRO The Netherlands
 */
package nl.vpro.web.filter.jsonp;

import java.util.Enumeration;
import java.util.StringTokenizer;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;

class RequestWrapper extends HttpServletRequestWrapper {

    private final String accept;

    public RequestWrapper(HttpServletRequest request, String a) {
        super(request);
        this.accept = a;
    }
    public RequestWrapper(HttpServletRequest request) {
        this(request, "application/json");
    }

    @Override
    public String getHeader(String header) {
        if(header.equalsIgnoreCase("accept")) {
            return accept;
        }
        return super.getHeader(header);
    }

    @Override
    public Enumeration getHeaders(String header) {
        if(header.equalsIgnoreCase("accept")) {
            StringTokenizer helper = new StringTokenizer(accept);
            return helper;
        }
        return super.getHeaders(header);
    }

}
