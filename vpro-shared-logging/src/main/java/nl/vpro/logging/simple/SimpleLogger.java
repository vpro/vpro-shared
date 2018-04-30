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
public interface  SimpleLogger<S extends SimpleLogger> extends BiConsumer<Level, String> {



    default S  debug(String format, Object... arg) {
        return log(Level.DEBUG, format, arg);
    }

    default S  info(String format, Object... arg) {
        return log(Level.INFO, format, arg);
    }

    default S  warn(String format, Object... arg) {
        return log(Level.WARN, format, arg);

    }

    default S  error(String format, Object... arg) {
        return log(Level.ERROR, format, arg);
    }


    default S  log(Level level, String format, Object... arg) {
        FormattingTuple ft = MessageFormatter.arrayFormat(format, arg);
        String message = ft.getMessage();
        if (ft.getArgArray().length == arg.length) {
            accept(level, message);
        } else if (arg.length > 1){
            Object t = arg[arg.length - 1];
            if (t instanceof Throwable) {
                accept(level, message, (Throwable) t);
            } else {
                accept(level, message);
            }
        }
        return self();
    }
    default S self() {
        return (S) this;
    }

    default S debugOrInfo(boolean info, String format, Object... arg) {
        return log(info ? Level.INFO : Level.DEBUG, format, arg);
    }

    @Override
    default void accept(Level level, String message) {
        accept(level, message, null);
    }
    void accept(Level level, String message, Throwable t);


}
