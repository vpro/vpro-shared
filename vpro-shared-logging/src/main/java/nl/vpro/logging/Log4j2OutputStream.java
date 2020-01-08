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

}
