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
        logger.atLevel(Level.valueOf(level.name())).log(txt);
    }

    /**
     * Log at the specified level. If the "logger" is null, nothing is logged.
     * If the "level" is null, nothing is logged. If the "txt" is null,
     * behaviour depends on the SLF4J implementation.
     * @deprecated Just use {@link Logger#atLevel(Level)}.{@link org.slf4j.spi.LoggingEventBuilder#log(String) log}
     */
    @Deprecated
    public static void log(Logger logger, Level level, String txt) {
        if (logger != null && level != null) {
            logger.atLevel(level).log(txt);
        }
    }


    public static void log(Logger logger, nl.vpro.logging.simple.Level level, String format, Object... argArray) {
        logger.atLevel(Level.valueOf(level.name())).log(format, argArray);
    }

    /**
     * Log at the specified level. If the "logger" is null, nothing is logged.
     * If the "level" is null, nothing is logged. If the "format" or the "argArray"
     * are null, behaviour depends on the SLF4J-backing implementation.
     */
    @Deprecated
    public static void log(Logger logger, Level level, String format, Object... argArray) {
        if (logger != null && level != null) {
            logger.atLevel(level).log(format, argArray);
        }
    }

    public static void debugOrInfo(Logger logger, boolean info, String format, Object... argArray) {
        log(logger, info ? Level.INFO : Level.DEBUG, format, argArray);
    }


    public static void log(Logger logger, nl.vpro.logging.simple.Level level, String txt, Throwable throwable) {
        logger.atLevel(Level.valueOf(level.name())).log(txt, throwable);
    }


    /**
     * Log at the specified level, with a Throwable on top. If the "logger" is null,
     * nothing is logged. If the "level" is null, nothing is logged. If the "format" or
     * the "argArray" or the "throwable" are null, behaviour depends on the SLF4J-backing
     * implementation.
     * @deprecated Just use {@link Logger#atLevel(Level)}
     */

    @SuppressWarnings("Duplicates")
    @Deprecated
    public static void log(Logger logger, Level level, String txt, Throwable throwable) {
        if (logger != null && level != null) {
            logger.atLevel(level).log(txt, throwable);
        }
    }

    /***
     * @deprecated Use {@link Logger#isEnabledForLevel(Level)} instead.
     */
    @Deprecated
    public static boolean isEnabled(@NonNull Logger logger, @NonNull Level level) {
        return logger.isEnabledForLevel(level);
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
