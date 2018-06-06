package nl.vpro.logging.simple;

import java.util.function.BiConsumer;

import org.slf4j.event.Level;
import org.slf4j.helpers.FormattingTuple;
import org.slf4j.helpers.MessageFormatter;


/**
 * A very simplified Logger. This can e.g. be used as messaging system. It was made to use in conjuction with @{link ChainedSimpleLogger} to be able to programmaticly 'tee' logging.
 *
 * The goal was to log to slf4j but also send corresponding messages to users via websockets.
 *
 * Generally this can be used when a Logger instance wants to be an argument, because simple loggers can be implemented easily, normally with a just a few lines, and actuall loggers can be wrapped easily too.
 *
 * @author Michiel Meeuwissen
 * @since 1.76
 */
public interface  SimpleLogger extends BiConsumer<Level, CharSequence> {


    default String getName() {
        return getClass().getSimpleName();
    }
    default void trace(String format, Object... arg) {
        log(Level.TRACE, format, arg);
    }


    default void debug(String format, Object... arg) {
        log(Level.DEBUG, format, arg);
    }

    default void info(String format, Object... arg) {
        log(Level.INFO, format, arg);
    }

    default void warn(String format, Object... arg) {
        log(Level.WARN, format, arg);

    }

    default void error(String format, Object... arg) {
        log(Level.ERROR, format, arg);
    }

     default void trace(CharSequence message) {
        log(Level.TRACE, message);
    }


    default void debug(CharSequence message) {
        log(Level.DEBUG, message);
    }

    default void info(CharSequence message) {
        log(Level.INFO, message);
    }

    default void warn(CharSequence message) {
        log(Level.WARN, message);

    }

    default void error(CharSequence message) {
        log(Level.ERROR, message);
    }

    default void log(Level level, CharSequence message) {
        accept(level, message);
    }


    default void log(Level level, String format, Object... arg) {
        if (isEnabled(level)) {
            FormattingTuple ft = MessageFormatter.arrayFormat(format, arg);
            String message = ft.getMessage();
            if (ft.getArgArray().length == arg.length) {
                accept(level, message, null);
            } else if (arg.length >= 1){
                Object t = arg[arg.length - 1];
                if (t instanceof Throwable) {
                    accept(level, message, (Throwable) t);
                } else {
                    accept(level, message, null);
                }
            } else {
                accept(Level.ERROR, "Format has " + ft.getArgArray().length + " args but " + arg.length + " were given", null);
                accept(level, message, null);
            }
        }
    }

    default void debugOrInfo(boolean info, String format, Object... arg) {
        log(info ? Level.INFO : Level.DEBUG, format, arg);
    }

    default boolean isEnabled(Level level) {
        return true;
    }

    @Override
    default void accept(Level level, CharSequence message) {
        accept(level, message, null);
    }
    void accept(Level level, CharSequence message, Throwable t);



}
