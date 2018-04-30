package nl.vpro.logging.simple;

import java.util.function.BiConsumer;

import org.slf4j.event.Level;
import org.slf4j.helpers.FormattingTuple;
import org.slf4j.helpers.MessageFormatter;


/**
 * A very simplified Logger. This can be used as messaging system. It was made to use in conjuction with @{link ChainedSimpleLogger} to be able to programmaticly 'tee' logging.
 *
 * The goal was to log to slf4j but also send corresponding messages to users via websockets.
 *
 * @author Michiel Meeuwissen
 * @since 1.76
 */
public interface  SimpleLogger extends BiConsumer<Level, String> {


    default void info(String format, Object... arg) {
        log(Level.INFO, format, arg);
    }

    default void error(String format, Object... arg) {
        log(Level.ERROR, format, arg);
    }

    default void debug(String format, Object... arg) {
        log(Level.DEBUG, format, arg);
    }

    default void log(Level level, String format, Object... arg) {
        FormattingTuple ft = MessageFormatter.arrayFormat(format, arg);
        String message = ft.getMessage();
        if (ft.getArgArray().length == arg.length) {
            accept(level, message);
        } else if (arg.length > 1){
            Object t = arg[arg.length - 1];
            if (t instanceof Throwable) {
                accept(level, message, (Throwable) t);
            }
        }
    }

    default void debugOrInfo(boolean level, String format, Object... arg) {
        log(level ? Level.INFO : Level.DEBUG, format, arg);
    }


    @Override
    default void accept(Level level, String message) {
        accept(level, message, null);
    }

    void accept(Level level, String message, Throwable t);


}
