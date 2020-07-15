package nl.vpro.logging;

import java.util.function.Function;

import org.apache.logging.log4j.Level;
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


    public static Log4j2OutputStream warn(Logger log) {
        return warn(log, false);
    }

    public static Log4j2OutputStream warn(Logger log, boolean skipEmptyLines) {
        return new Log4j2OutputStream(skipEmptyLines) {
            @Override
            void log(String line) {
                log.warn(line);
            }
        };
    }

    public static Log4j2OutputStream error(Logger log) {
        return error(log, false);
    }

    public static Log4j2OutputStream error(Logger log, boolean skipEmptyLines) {
        return new Log4j2OutputStream(skipEmptyLines) {
            @Override
            void log(String line) {
                log.error(line);
            }
        };
    }


    public static LoggerOutputStream log(final Logger log, Function<String, Level> level) {
        return new LoggerOutputStream(false) {
            @Override
            void log(String line) {
                 Level l = level.apply(line);
                if (l != null) {
                    log.log(l, line);
                }
            }
        };
    }


    Log4j2OutputStream(boolean skipEmptyLines) {
        super(skipEmptyLines, null);
    }

}
