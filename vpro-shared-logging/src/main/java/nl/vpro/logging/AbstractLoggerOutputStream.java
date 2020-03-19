package nl.vpro.logging;

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

    AbstractLoggerOutputStream(boolean skipEmptyLines) {
        this.skipEmptyLines = skipEmptyLines;
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
    public void write(byte[] b, int off, int len) throws IOException {
        int blockoffset = off;
        int blocklen = 0;
        for (int i = 0; i < len; i++) {
            if (b[i] == '\n') {
                if (blocklen > 0) {
                    buffer.write(b, blockoffset, blocklen);
                    blockoffset += blocklen;
                    blocklen = 0;
                    flush();
                }
            } else {
                blocklen++;
            }
        }
        if (blocklen > 0) {
            buffer.write(b, blockoffset, blocklen);
        }
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
        if (! skipEmpty || ! line.isEmpty()) {
            log(line);
        }
        buffer.reset();
    }
}
