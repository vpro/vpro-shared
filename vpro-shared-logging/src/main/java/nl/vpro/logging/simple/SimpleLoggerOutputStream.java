package nl.vpro.logging.simple;

import nl.vpro.logging.AbstractLoggerOutputStream;

public abstract class SimpleLoggerOutputStream extends AbstractLoggerOutputStream {

    public SimpleLoggerOutputStream(boolean skipEmptyLines, Integer max) {
        super(skipEmptyLines, max);
    }

    public SimpleLoggerOutputStream(boolean skipEmptyLines) {
        super(skipEmptyLines, null);
    }

    public static SimpleLoggerOutputStream info(SimpleLogger log) {
        return info(log, false);
    }

    public static SimpleLoggerOutputStream info(SimpleLogger log, boolean skipEmptyLines) {
        return new SimpleLoggerOutputStream(skipEmptyLines) {
            @Override
            protected void log(String line) {
                log.info(line);
            }
        };
    }

}
