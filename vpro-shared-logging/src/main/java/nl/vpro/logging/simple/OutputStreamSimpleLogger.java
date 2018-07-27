package nl.vpro.logging.simple;

import lombok.Getter;
import lombok.SneakyThrows;

import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.util.function.Function;

import org.slf4j.event.Level;

/**
 * @author Michiel Meeuwissen
 * @since ...
 */
public class OutputStreamSimpleLogger extends AbstractStringBuilderSimpleLogger {

    @Getter
    private final OutputStream outputStream;

    long size = 0L;

    @lombok.Builder
    private OutputStreamSimpleLogger(
        OutputStream output,
        Level level,
        Long maxLength,
        Function<Level, String> prefix) {
        super(level, maxLength, prefix);
        this.outputStream = output == null ? new ByteArrayOutputStream() : output;
    }

     public OutputStreamSimpleLogger() {
        this(null, null, null, null);
    }



    @Override
    int getLength() {
        return (int) size;

    }

    @Override
    @SneakyThrows
    void append(CharSequence m) {
        byte[] s = m.toString().getBytes();
        size += s.length;
        outputStream.write(s);
    }

    @Override
    @SneakyThrows
    void append(char c) {
        size++;
        outputStream.write(c);
    }

    @Override
    void truncateIfNecessary() {
    }
}