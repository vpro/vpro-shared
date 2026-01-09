package nl.vpro.util;

import lombok.Getter;
import lombok.Setter;

import java.io.*;


/**
 * A wrapper for an {@link InputStream} that logs it's first bytes.
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
    void closed(long count, boolean truncated) throws IOException {
        fileOutputStream.close();
    }

}
