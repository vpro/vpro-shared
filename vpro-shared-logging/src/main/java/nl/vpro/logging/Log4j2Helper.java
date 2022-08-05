package nl.vpro.logging;


import java.time.Duration;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.Logger;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.slf4j.LoggerFactory;
import org.slf4j.helpers.FormattingTuple;
import org.slf4j.helpers.MessageFormatter;

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

    public static void log(Logger log, nl.vpro.logging.simple.Level level, CharSequence format, Object... argArray) {
        log.log(Level.valueOf(level.name()), format.toString(), argArray);
    }

    public static void log(Logger log, Duration duration, Duration durationInfo, String format, Object... argArray) {
        log.log(getLevel(duration, durationInfo.dividedBy(2), durationInfo, durationInfo.multipliedBy(2), durationInfo.multipliedBy(3)), format, argArray);
    }

    public static org.slf4j.Logger slf4j(Logger logger) {
        return LoggerFactory.getLogger(logger.getName());
    }


    public static String returnAndWarn(@NonNull Logger logger, @NonNull String format, Object... arg) {
        FormattingTuple ft = MessageFormatter.arrayFormat(format, arg);
        String message = ft.getMessage();
        logger.warn(message);
        return message;
    }


    public static String returnAndInfo(@NonNull Logger logger, @NonNull String format, Object... arg) {
        FormattingTuple ft = MessageFormatter.arrayFormat(format, arg);
        String message = ft.getMessage();
        logger.info(message);
        return message;
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
