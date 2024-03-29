package nl.vpro.logging;

import org.checkerframework.checker.nullness.qual.NonNull;

import org.slf4j.Logger;
import org.slf4j.event.Level;
import org.slf4j.helpers.FormattingTuple;
import org.slf4j.helpers.MessageFormatter;

/**
 *  <a href="https://stackoverflow.com/questions/2621701/setting-log-level-of-message-at-runtime-in-slf4j">See stackoverflow</a>
 * @author Michiel Meeuwissen
 * @since 1.77
 */
public class Slf4jHelper {

    private Slf4jHelper() {
    }



    public static void log(Logger logger, nl.vpro.logging.simple.Level level, String txt) {
        log(logger, Level.valueOf(level.name()), txt);
    }

    /**
     * Log at the specified level. If the "logger" is null, nothing is logged.
     * If the "level" is null, nothing is logged. If the "txt" is null,
     * behaviour depends on the SLF4J implementation.
     */

    public static void log(Logger logger, Level level, String txt) {
        if (logger != null && level != null) {
            switch (level) {
            case TRACE:
                logger.trace(txt);
                break;
            case DEBUG:
                logger.debug(txt);
                break;
            case INFO:
                logger.info(txt);
                break;
            case WARN:
                logger.warn(txt);
                break;
            case ERROR:
                logger.error(txt);
                break;
            }
        }
    }


    public static void log(Logger logger, nl.vpro.logging.simple.Level level, String format, Object... argArray) {
        log(logger, Level.valueOf(level.name()), format, argArray);
    }

    /**
     * Log at the specified level. If the "logger" is null, nothing is logged.
     * If the "level" is null, nothing is logged. If the "format" or the "argArray"
     * are null, behaviour depends on the SLF4J-backing implementation.
     */

    @SuppressWarnings("Duplicates")
    public static void log(Logger logger, Level level, String format, Object... argArray) {
        if (logger != null && level != null) {
            switch (level) {
            case TRACE:
                logger.trace(format, argArray);
                break;
            case DEBUG:
                logger.debug(format, argArray);
                break;
            case INFO:
                logger.info(format, argArray);
                break;
            case WARN:
                logger.warn(format, argArray);
                break;
            case ERROR:
                logger.error(format, argArray);
                break;
            }
        }
    }

    public static void debugOrInfo(Logger logger, boolean info, String format, Object... argArray) {
        log(logger, info ? Level.INFO : Level.DEBUG, format, argArray);
    }


    public static void log(Logger logger, nl.vpro.logging.simple.Level level, String txt, Throwable throwable) {
        log(logger, Level.valueOf(level.name()), txt, throwable);
    }


    /**
     * Log at the specified level, with a Throwable on top. If the "logger" is null,
     * nothing is logged. If the "level" is null, nothing is logged. If the "format" or
     * the "argArray" or the "throwable" are null, behaviour depends on the SLF4J-backing
     * implementation.
     */

    @SuppressWarnings("Duplicates")
    public static void log(Logger logger, Level level, String txt, Throwable throwable) {
        if (logger != null && level != null) {
            switch (level) {
            case TRACE:
                logger.trace(txt, throwable);
                break;
            case DEBUG:
                logger.debug(txt, throwable);
                break;
            case INFO:
                logger.info(txt, throwable);
                break;
            case WARN:
                logger.warn(txt, throwable);
                break;
            case ERROR:
                logger.error(txt, throwable);
                break;
            }
        }
    }

    public static boolean isEnabled(@NonNull Logger logger, @NonNull Level level) {
        switch (level) {
            case TRACE:
                return logger.isTraceEnabled();
            case DEBUG:
                return logger.isDebugEnabled();
            case INFO:
                return logger.isInfoEnabled();
            case WARN:
                return logger.isWarnEnabled();
            case ERROR:
                return logger.isErrorEnabled();
            default:
                return true;
        }
    }


    public static String returnAndWarn(@NonNull  Logger logger, @NonNull String format, Object... arg) {
        FormattingTuple ft = MessageFormatter.arrayFormat(format, arg);
        String message = ft.getMessage();
        logger.warn(message);
        return message;
    }


    public static String returnAndInfo(@NonNull  Logger logger, @NonNull String format, Object... arg) {
        FormattingTuple ft = MessageFormatter.arrayFormat(format, arg);
        String message = ft.getMessage();
        logger.info(message);
        return message;
    }

}
