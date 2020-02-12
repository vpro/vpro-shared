package nl.vpro.logging;

import org.apache.logging.log4j.Logger;

/**
 * @author Michiel Meeuwissen
 * @since 2.10
 */
public abstract class Log4j2OutputStream {

    public static LoggerOutputStream debug(Logger log) {
        return debug(log, false);
    }

    public static LoggerOutputStream debug(Logger log, boolean skipEmptyLines) {
        return new LoggerOutputStream(skipEmptyLines) {
            @Override
            void log(String line) {
                log.debug(line);
            }
        };
    }

    public static LoggerOutputStream info(Logger log) {
        return info(log, false);
    }

    public static LoggerOutputStream info(Logger log, boolean skipEmptyLines) {
        return new LoggerOutputStream(skipEmptyLines) {
            @Override
            void log(String line) {
                log.info(line);
            }
        };
    }

}
