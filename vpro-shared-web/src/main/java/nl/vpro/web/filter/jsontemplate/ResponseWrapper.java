/*
 * Copyright (C) 2010 All rights reserved
 * VPRO The Netherlands
 */
package nl.vpro.web.filter.jsontemplate;

import nl.vpro.web.support.WrappedServletOutputStream;

import javax.servlet.ServletOutputStream;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServletResponseWrapper;
import java.io.*;
import java.nio.charset.Charset;


/**
 * TODO needs to buffer the entire response. Twice.
 */
class ResponseWrapper extends HttpServletResponseWrapper {

    private final ByteArrayOutputStream buffer = new ByteArrayOutputStream();

    private final String encoding;

    private final byte[] prefix;

    private final byte[] suffix;

    public ResponseWrapper(HttpServletResponse response, String property) throws UnsupportedEncodingException {
        super(response);

        encoding = getResponse().getCharacterEncoding();
        prefix = ("{ \"" + property + "\" : \"").getBytes(encoding);
        suffix = "\"}".getBytes(encoding);

    }

    @Override
    public PrintWriter getWriter() throws IOException {
        return new PrintWriter(getOutputStream(), true);
    }

    @Override
    public ServletOutputStream getOutputStream() {
        return new WrappedServletOutputStream(buffer);
    }

    public void flush() throws IOException {
        final ByteArrayOutputStream tmp = new ByteArrayOutputStream();

        final Charset charset = Charset.forName(encoding);

        final String content = new String(buffer.toByteArray(), charset);
        for(int i = 0; i < content.length(); i++) {
            char ch = content.charAt(i);
            String escaped = escape(ch);
            tmp.write(escaped.getBytes(charset));
        }

        final int length = tmp.size() + prefix.length + suffix.length;
        final ServletResponse response = getResponse();
        response.setContentType("application/json");
        response.setContentLength(length);

        OutputStream out = response.getOutputStream();
        out.write(prefix);
        tmp.writeTo(out);
        out.write(suffix);
        out.close();
    }

    private static String escape(char ch) {
        StringBuilder sb = new StringBuilder(8);

        // http://json.org/
        switch(ch) {
            case '"':
                sb.append("\\\"");
                break;
            case '\\':
                sb.append("\\\\");
                break;
            case '/':
                sb.append("\\/");
                break;
            case '\b':
                sb.append("\\b");
                break;
            case '\f':
                sb.append("\\f");
                break;
            case '\n':
                sb.append("\\n");
                break;
            case '\r':
                sb.append("\\r");
                break;
            case '\t':
                sb.append("\\t");
                break;
            default:
                if((ch >= '\u0000' && ch <= '\u001F') || (ch >= '\u007F' && ch <= '\u009F') || (ch >= '\u2000' && ch <= '\u20FF')) {
                    String hex = Integer.toHexString(ch);
                    sb.append("\\u");
                    for(int k = 0; k < 4 - hex.length(); k++) {
                        sb.append('0');
                    }
                    sb.append(hex.toUpperCase());
                } else {
                    sb.append(ch);
                }
        }
        return sb.toString();
    }
}
