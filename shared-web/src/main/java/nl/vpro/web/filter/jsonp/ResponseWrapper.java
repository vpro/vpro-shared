/**
 * Copyright (C) 2010 All rights reserved
 * VPRO The Netherlands
 */
package nl.vpro.web.filter.jsonp;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServletResponseWrapper;
import java.io.*;

public class ResponseWrapper extends HttpServletResponseWrapper {

    private ByteArrayOutputStream output;

    private byte[] prefix;

    private byte[] suffix;

    private int increment;

    public ResponseWrapper(HttpServletResponse response) throws UnsupportedEncodingException {
        this(response, "callback");
    }

    public ResponseWrapper(HttpServletResponse response, String callback) throws UnsupportedEncodingException {
        super(response);
        output = new ByteArrayOutputStream();

        String encoding = getResponse().getCharacterEncoding();

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
        return new ServletStreamImpl(output);
    }


    public void flush() throws IOException {
        OutputStream out = getResponse().getOutputStream();
        out.write(prefix);
        out.write(output.toByteArray());
        out.write(suffix);
        out.close();

        getResponse().setContentType("application/javascript");
        getResponse().setContentLength(output.size() + increment);

    }
}
