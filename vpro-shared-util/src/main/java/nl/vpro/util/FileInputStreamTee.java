package nl.vpro.util;

import lombok.Getter;
import lombok.Setter;

import java.io.*;
import java.util.function.BiConsumer;

import org.meeuw.functional.Consumers;


/**
 * A wrapper for an {@link InputStream} that logs it's first bytes.
 */
@Setter
@Getter
public class FileInputStreamTee extends TruncatedObservableInputStream {

    private final OutputStream fileOutputStream;

    private final BiConsumer<Long, Boolean> consumer;

    public FileInputStreamTee(OutputStream fileOutputStream, InputStream wrapped, BiConsumer<Long, Boolean> consumer) {
        super(wrapped);
        this.fileOutputStream = fileOutputStream;
        this.consumer = consumer;
    }
    public FileInputStreamTee(OutputStream fileOutputStream, InputStream wrapped) {
        this(fileOutputStream, wrapped, Consumers.biNop());
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
        this.consumer.accept(count, truncated);
    }

}
