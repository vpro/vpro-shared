/*
 * Copyright (C) 2010 All rights reserved
 * VPRO The Netherlands
 */
package nl.vpro.web.filter.jsontemplate;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;
import java.util.Enumeration;
import java.util.StringTokenizer;

class RequestWrapper extends HttpServletRequestWrapper {

    private String accept;

    public RequestWrapper(HttpServletRequest request) {
        super(request);
        this.accept = "application/json";
    }

    @Override
    public String getHeader(String header) {
        if(header.toLowerCase().equals("accept")) {
            return accept;
        }
        return super.getHeader(header);
    }

    @Override
    public Enumeration getHeaders(String header) {
        if(header.toLowerCase().equals("accept")) {
            StringTokenizer helper = new StringTokenizer(accept);
            return helper;
        }
        return super.getHeaders(header);
    }

    public void setAccept(String accept) {
        this.accept = accept;
    }
}
