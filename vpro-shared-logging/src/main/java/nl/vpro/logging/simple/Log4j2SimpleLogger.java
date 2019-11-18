package nl.vpro.logging.simple;


import org.apache.logging.log4j.Logger;
import org.slf4j.event.Level;

/**
 *  SimpleLogger that wraps log4j2's {@link Logger}.
 * @author Michiel Meeuwissen
 * @since 2.9
 */
public class Log4j2SimpleLogger implements SimpleLogger {
    private final Logger log;

    public Log4j2SimpleLogger(Logger log) {
        this.log = log;
    }

    public static Log4j2SimpleLogger of(Logger log) {
        return new Log4j2SimpleLogger(log);
    }

    @Override
    public void accept(Level level, CharSequence message, Throwable t) {
        log.log(toLevel(level), message, t);
    }

    @Override
    public boolean isEnabled(Level level) {
        return log.isEnabled(toLevel(level));
    }

    public static org.apache.logging.log4j.Level toLevel(Level level) {
        switch(level) {
            case TRACE:
                return org.apache.logging.log4j.Level.TRACE;
            case DEBUG:
                return org.apache.logging.log4j.Level.DEBUG;
            case INFO:
                return org.apache.logging.log4j.Level.INFO;
            case WARN:
                return org.apache.logging.log4j.Level.WARN;
            case ERROR:
            default:
                return org.apache.logging.log4j.Level.ERROR;
        }
    }

}
