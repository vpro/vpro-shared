package nl.vpro.logging;

import lombok.Getter;
import lombok.Setter;

import java.io.*;

import org.slf4j.Logger;

/**
 * Wraps a {@link Logger} in an {@link OutputStream}, making logging available as an outputstream, which can be useful for things that accept outputstreams (e.g. external processes)
 * @author Michiel Meeuwissen
 */
abstract class AbstractLoggerOutputStream extends OutputStream {

    final ByteArrayOutputStream buffer = new ByteArrayOutputStream();
    final boolean skipEmptyLines;
    int lastChar = -1;

    @Getter
    protected long count = 0;

    @Getter
    @Setter
    protected Integer max;

    AbstractLoggerOutputStream(boolean skipEmptyLines, Integer max) {
        this.skipEmptyLines = skipEmptyLines;
        this.max = max;
    }

    abstract void log(String line);


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

    private void log(boolean skipEmpty) {
        String line = buffer.toString();
        try {
            if (!skipEmpty || !line.isEmpty()) {
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
