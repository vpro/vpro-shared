package nl.vpro.util;

import lombok.Getter;
import lombok.Setter;

import java.io.*;


/**
 * A wrapper for an {@link InputStream} that writes its first bytes to an output stream.
 */
@Setter
@Getter
public class FileInputStreamTee extends TruncatedObservableInputStream {

    private final OutputStream fileOutputStream;

    public FileInputStreamTee(OutputStream fileOutputStream, InputStream wrapped) {
        super(wrapped);
        this.fileOutputStream = fileOutputStream;
    }

    @Override
    void write(byte[] buffer, int offset, int effectiveLength) throws IOException {
        fileOutputStream.write(buffer, offset, effectiveLength);
    }

    @Override
    void write(int value) throws IOException {
        fileOutputStream.write(value);
    }

    @Override
    public void close() throws IOException {
        IOException exception = null;
        try {
            super.close();
        } catch (IOException e) {
            exception = e;
        }

        try {
            fileOutputStream.close();
        } catch (IOException e) {
            if (exception == null) {
                exception = e;
            } else {
                exception.addSuppressed(e);
            }
        }

        if (exception != null) {
            throw exception;
        }
    }

}
