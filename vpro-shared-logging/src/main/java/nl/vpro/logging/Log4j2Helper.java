package nl.vpro.logging;


import java.time.Duration;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.Logger;

/**
 * @author Michiel Meeuwissen
 * @since 2.12
 */
public class Log4j2Helper {

    private Log4j2Helper() {
    }


    public static void debugOrInfo(Logger logger, boolean info, String format, Object... argArray) {
        logger.log(info ? Level.INFO : Level.DEBUG, format, argArray);
    }

    public static void log(Logger log, nl.vpro.logging.simple.Level level, String format, Object... argArray) {
        log.log(Level.valueOf(level.name()), format, argArray);
    }

    public static void log(Logger log, Duration duration, Duration durationInfo, String format, Object... argArray) {
        log.log(getLevel(duration, durationInfo.dividedBy(2), durationInfo, durationInfo.multipliedBy(2), durationInfo.multipliedBy(3)), format, argArray);
    }

    private  static Level getLevel(Duration duration, Duration durationDebug, Duration durationInfo, Duration durationWarn, Duration durationError) {
        if (duration.compareTo(durationDebug) < 0 ) {
            return Level.TRACE;
        } else if (duration.compareTo(durationInfo) < 0) {
            return Level.DEBUG;
        } else if (duration.compareTo(durationWarn) < 0) {
            return Level.INFO;
        } else if (duration.compareTo(durationError) < 0) {
            return Level.WARN;
        } else {
            return Level.ERROR;
        }
    }
}
