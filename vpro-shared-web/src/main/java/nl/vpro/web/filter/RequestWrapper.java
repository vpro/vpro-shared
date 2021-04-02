/*
 * Copyright (C) 2010 All rights reserved
 * VPRO The Netherlands
 */
package nl.vpro.web.filter;

import java.util.*;
import java.util.stream.Collectors;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;

public class RequestWrapper extends HttpServletRequestWrapper {

    private final List<String> accept;

    public RequestWrapper(HttpServletRequest request, String a) {
        super(request);
        this.accept = Collections.list(new StringTokenizer(a)).stream().map(t -> (String) t).collect(Collectors.toList());
    }
    public RequestWrapper(HttpServletRequest request) {
        this(request, "application/json");
    }

    @Override
    public String getHeader(String header) {
        if(header.equalsIgnoreCase("accept")) {
            return String.join(",", accept);
        }
        return super.getHeader(header);
    }

    @Override
    public Enumeration<String> getHeaders(String header) {
        if(header.equalsIgnoreCase("accept")) {
            return Collections.enumeration(this.accept);
        }
        return super.getHeaders(header);
    }

}
