package nl.vpro.logging.simple;

import lombok.Getter;
import lombok.SneakyThrows;

import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.util.function.Function;

import org.slf4j.event.Level;

/**
 * @author Michiel Meeuwissen
 * @since 1.79
 */
public class OutputStreamSimpleLogger extends AbstractStringBuilderSimpleLogger {

    @Getter
    private final OutputStream outputStream;

    private final Charset charset;

    long size = 0L;

    boolean autoFlush = false;

    @lombok.Builder
    private OutputStreamSimpleLogger(
        OutputStream output,
        Level level,
        Long maxLength,
        Function<Level, String> prefix,
        boolean autoFlush,
        Charset charset
    ) {
        super(level, maxLength, prefix);
        this.outputStream = output == null ? new ByteArrayOutputStream() : output;
        this.autoFlush = autoFlush;
        this.charset = charset == null ? Charset.defaultCharset() : charset;
    }

     public OutputStreamSimpleLogger() {
        this(null, null, null, null, false, Charset.defaultCharset());
    }



    @Override
    int getLength() {
        return (int) size;

    }

    @Override
    @SneakyThrows
    void append(CharSequence m) {
        byte[] s = m.toString().getBytes(charset);
        size += s.length;
        outputStream.write(s);
    }

    @Override
    protected boolean needsNewLine() {
        if (autoFlush) {
            return false;
        } else {
            return super.needsNewLine();
        }
    }

    @Override
    @SneakyThrows
    void append(char c) {
        size++;
        outputStream.write(c);
    }

    @Override
    @SneakyThrows
    void truncateIfNecessary() {
        if (autoFlush) {
            outputStream.write('\n');
            outputStream.flush();
            count++;
        }
    }
}
