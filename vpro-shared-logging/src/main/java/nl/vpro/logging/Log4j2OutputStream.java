package nl.vpro.logging;

import org.apache.logging.log4j.Logger;

/**
 * @author Michiel Meeuwissen
 * @since 2.10
 */
public abstract class Log4j2OutputStream extends AbstractLoggerOutputStream {

    public static Log4j2OutputStream debug(Logger log) {
        return debug(log, false);
    }

    public static Log4j2OutputStream debug(Logger log, boolean skipEmptyLines) {
        return new Log4j2OutputStream(skipEmptyLines) {
            @Override
            void log(String line) {
                log.debug(line);
            }
        };
    }

    public static Log4j2OutputStream info(Logger log) {
        return info(log, false);
    }

    public static Log4j2OutputStream info(Logger log, boolean skipEmptyLines) {
        return new Log4j2OutputStream(skipEmptyLines) {
            @Override
            void log(String line) {
                log.info(line);
            }
        };
    }

    Log4j2OutputStream(boolean skipEmptyLines) {
        super(skipEmptyLines);
    }

}
