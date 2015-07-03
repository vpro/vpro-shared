/**
 * Copyright (C) 2010 All rights reserved
 * VPRO The Netherlands
 */
package nl.vpro.web.filter.jsonp;

import java.io.*;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServletResponseWrapper;

import nl.vpro.web.support.WrappedServletOutputStream;

class ResponseWrapper extends HttpServletResponseWrapper {

    private final ByteArrayOutputStream buffer;

    private final byte[] prefix;

    private final byte[] suffix;

    private final int increment;

    public ResponseWrapper(HttpServletResponse response) throws UnsupportedEncodingException {
        this(response, "callback");
    }

    public ResponseWrapper(HttpServletResponse response, String callback) throws UnsupportedEncodingException {
        super(response);
        buffer = new ByteArrayOutputStream();

        final String encoding = response.getCharacterEncoding();

        prefix = (callback + "(").getBytes(encoding);
        suffix = ");".getBytes(encoding);
        increment = prefix.length + suffix.length;
    }

    @Override
    public PrintWriter getWriter() throws IOException {
        return new PrintWriter(getOutputStream(), true);
    }

    @Override
    public ServletOutputStream getOutputStream() throws IOException {
        return new WrappedServletOutputStream(buffer);
    }

    void flush() throws IOException {
        getResponse().setContentType("application/javascript");
        getResponse().setContentLength(buffer.size() + increment);

        OutputStream out = getResponse().getOutputStream();
        out.write(prefix);
        buffer.writeTo(out);
        out.write(suffix);
        out.close();
    }
}
