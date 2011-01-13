/**
 * Copyright (C) 2010 All rights reserved
 * VPRO The Netherlands
 */
package nl.vpro.web.support;

import javax.servlet.ServletOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.OutputStream;

/**
 * To wrap the output stream we need an implementation of a ServletOutputStream
 */
public class WrappedServletOutputStream extends ServletOutputStream {
    
    private DataOutputStream stream;

    public WrappedServletOutputStream(OutputStream stream) {
        this.stream = new DataOutputStream(stream);
    }

    public void write(int b) throws IOException {
        stream.write(b);
    }
    
    public void write(byte[] b) throws IOException {
        stream.write(b);
    }

    public void write(byte[] b, int off, int len) throws IOException {
        stream.write(b, off, len);
    }
}
