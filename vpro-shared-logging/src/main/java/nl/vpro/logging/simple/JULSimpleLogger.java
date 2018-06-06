package nl.vpro.logging.simple;

import java.util.logging.Logger;

import org.slf4j.event.Level;

/**
 * SimpleLogger that wraps a java.util.logger.
 * @author Michiel Meeuwissen
 * @since 1.79
 */
public class JULSimpleLogger implements SimpleLogger {
    private final Logger log;

    public JULSimpleLogger(Logger log) {
        this.log = log;
    }

    @Override
    public void accept(Level level, CharSequence message, Throwable t) {
        log.log(toLevel(level), message.toString(), t);
    }


    @Override
    public boolean isEnabled(Level level) {
        return log.getLevel().intValue() <= toLevel(level).intValue();
    }

    public static java.util.logging.Level toLevel(Level level) {
        switch(level) {
            case TRACE:
                return java.util.logging.Level.FINEST;
            case DEBUG:
                return java.util.logging.Level.FINE;
            case INFO:
                return java.util.logging.Level.INFO;
            case WARN:
                return java.util.logging.Level.WARNING;
            case ERROR:
            default:
                return java.util.logging.Level.SEVERE;
        }
    }

}
