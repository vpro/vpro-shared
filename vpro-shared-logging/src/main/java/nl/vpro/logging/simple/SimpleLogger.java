package nl.vpro.logging.simple;

import java.util.function.BiConsumer;

import org.slf4j.event.Level;
import org.slf4j.helpers.FormattingTuple;
import org.slf4j.helpers.MessageFormatter;


/**
 * A very simplified Logger. This can be used as messaging system. It was made to use in conjuction with @{link ChainedSimpleLogger} to be able to programmaticly 'tee' logging.
 *
 * The goal was to log to slf4j but also send corresponding messages to users via websockets.
 * @author Michiel Meeuwissen
 * @since 1.76
 */
public interface  SimpleLogger extends  BiConsumer<Level, String> {


     default void info(String format, Object... arg) {
         FormattingTuple ft = MessageFormatter.arrayFormat(format, arg);
         String message = ft.getMessage();
         accept(Level.INFO, message);
     }


     default void error(String format, Object... arg) {
         FormattingTuple ft = MessageFormatter.arrayFormat(format, arg);
         String message = ft.getMessage();
         accept(Level.ERROR, message);
     }

    @Override
    default void accept(Level level, String message) {
        accept(level, message, null);
    }

    void accept(Level level, String message, Throwable t);


}
