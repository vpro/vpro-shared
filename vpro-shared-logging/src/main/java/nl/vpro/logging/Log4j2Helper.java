package nl.vpro.logging;


import java.time.Duration;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.Logger;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.slf4j.LoggerFactory;
import org.slf4j.helpers.FormattingTuple;
import org.slf4j.helpers.MessageFormatter;

/**
 * @author Michiel Meeuwissen
 * @since 2.12
 */
public class Log4j2Helper {

    public static final int DURATION_FACTOR = 2;

    private Log4j2Helper() {
    }

    public static void debugOrInfo(Logger logger, boolean info, String format, Object... argArray) {
        logger.log(info ? Level.INFO : Level.DEBUG, format, argArray);
    }

    public static void log(Logger log, nl.vpro.logging.simple.Level level, CharSequence format, Object... argArray) {
        log.log(Level.valueOf(level.name()), format.toString(), argArray);
    }

    public static void log(Logger log, org.slf4j.event.Level level, CharSequence format, Object... argArray) {
        log.log(Level.valueOf(level.name()), format.toString(), argArray);
    }

    /**
     * Issues a log entry, where the level is dependent on 2 durations
     * @param duration The duration something took
     * @param durationInfo A duration to compare with. The used level will be {@link Level#INFO} if duration bigger than this. Other thresholds are multiples of this.
     */
    public static void log(Logger log, Duration duration, Duration durationInfo, String format, Object... argArray) {
        log.log(
            getLevel(
                duration,
                durationInfo.dividedBy(DURATION_FACTOR),
                durationInfo,
                durationInfo.multipliedBy(DURATION_FACTOR),
                durationInfo.multipliedBy(DURATION_FACTOR * DURATION_FACTOR)
            ),
            format, argArray);
    }

    /**
     * As {@link #log(Logger, nl.vpro.logging.simple.Level, CharSequence, Object...)}, but there will never by issued a {@link Level#ERROR}. At most, it will be {@link Level#WARN}
     */
    public static void logWarnAtMost(Logger log, Duration duration, Duration durationInfo, String format, Object... argArray) {
        log.log(
            getLevel(
                duration,
                durationInfo.dividedBy(DURATION_FACTOR),
                durationInfo,
                durationInfo.multipliedBy(DURATION_FACTOR),
                null
            ),
            format, argArray);
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

    private  static Level getLevel(
        Duration duration,
        Duration durationDebug,
        Duration durationInfo,
        @Nullable Duration durationWarn,
        @Nullable Duration durationError) {
        if (duration.compareTo(durationDebug) < 0 ) {
            return Level.TRACE;
        } else if (duration.compareTo(durationInfo) < 0) {
            return Level.DEBUG;
        } else if (durationWarn == null || duration.compareTo(durationWarn) < 0) {
            return Level.INFO;
        } else if (durationError == null || duration.compareTo(durationError) < 0) {
            return Level.WARN;
        } else {
            return Level.ERROR;
        }
    }

}
