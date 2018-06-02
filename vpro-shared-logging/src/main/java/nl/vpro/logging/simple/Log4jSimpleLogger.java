package nl.vpro.logging.simple;


import org.apache.log4j.Logger;
import org.slf4j.event.Level;

/**
 *  SimpleLogger that wraps {@link org.apache.log4j.Logger}
 * @author Michiel Meeuwissen
 * @since 1.79
 */
public class Log4jSimpleLogger implements SimpleLogger<Log4jSimpleLogger> {
    private final Logger log;

    public Log4jSimpleLogger(Logger log) {
        this.log = log;
    }

    @Override
    public void accept(Level level, String message, Throwable t) {
        log.log(toLevel(level), message, t);
    }

    @Override
    public boolean isEnabled(Level level) {
        return log.isEnabledFor(toLevel(level));
    }

    public static org.apache.log4j.Level toLevel(Level level) {
        switch(level) {
            case TRACE:
                return org.apache.log4j.Level.TRACE;
            case DEBUG:
                return org.apache.log4j.Level.DEBUG;
            case INFO:
                return org.apache.log4j.Level.INFO;
            case WARN:
                return org.apache.log4j.Level.WARN;
            case ERROR:
            default:
                return org.apache.log4j.Level.ERROR;
        }
    }

}
