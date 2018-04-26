package nl.vpro.logging.simple;

import org.slf4j.Logger;
import org.slf4j.event.Level;

import nl.vpro.logging.Slf4jHelper;

/**
 * Wraps an SLF4J {@link Logger}

 * @author Michiel Meeuwissen
 * @since 1.76
 */
public class Slf4jSimpleLogger implements SimpleLogger {

    private final Logger logger;

    public Slf4jSimpleLogger(Logger logger) {
        this.logger = logger;
    }

    public static SimpleLogger of(Logger logger) {
        return new Slf4jSimpleLogger(logger);
    }

    @Override
    public void accept(Level level, String message, Throwable t) {
        Slf4jHelper.log(logger, level, message, t);
    }
}
