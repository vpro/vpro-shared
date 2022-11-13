package nl.vpro.logging;

import lombok.*;

import java.io.*;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

import org.apache.commons.lang3.StringUtils;

/**
 * Wraps some logger in an {@link OutputStream}, making logging available as an outputstream, which can be useful for things that accept outputstreams (e.g. external processes)
 * @author Michiel Meeuwissen
 */
public abstract class AbstractLoggerOutputStream extends OutputStream {

    private final ByteArrayOutputStream buffer = new ByteArrayOutputStream();
    private final boolean skipEmptyLines;
    private int lastChar = -1;

    @Getter
    protected long count = 0;

    @Getter
    @Setter
    protected Integer max;

    @Getter
    @Setter
    protected Charset charset = StandardCharsets.UTF_8;

    protected AbstractLoggerOutputStream(boolean skipEmptyLines, Integer max) {
        this.skipEmptyLines = skipEmptyLines;
        this.max = max;
    }

    protected abstract void log(String line);

    @Override
    public void write(int b) {
        switch(b) {
            case '\n':
                log(skipEmptyLines);
                break;
            case '\r':
                if (lastChar != '\n') {
                    log(skipEmptyLines);
                }
                break;
            default:
                buffer.write(b);
        }
        lastChar = b;
    }

    @Override
    public void flush() {
        log(skipEmptyLines);
    }

    @Override
    public void close() throws IOException {
        super.close();
        log(true);
    }

    @SneakyThrows
    private void log(boolean skipEmpty) {
        final String line = buffer.toString(charset.name());
        try {
            if (!skipEmpty || StringUtils.isNotBlank(line)) {
                count++;
                if (max != null) {
                    if (count > max) {
                        if (count == max + 1) {
                            log("...");
                        }
                        return;
                    }
                }
                log(line);
            }
        } finally {
            buffer.reset();
        }
    }
}
