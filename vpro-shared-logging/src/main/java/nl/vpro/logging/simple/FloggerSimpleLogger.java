package nl.vpro.logging.simple;

import org.slf4j.event.Level;

import com.google.common.flogger.FluentLogger;

/**
 * @author Michiel Meeuwissen
 * @since 1.79
 */
public class FloggerSimpleLogger implements SimpleLogger<FloggerSimpleLogger> {
    private final FluentLogger logger;

    public FloggerSimpleLogger(FluentLogger logger) {
        this.logger = logger;
    }

    @Override
    public void accept(Level level, String message, Throwable t) {
        logger.at(JULSimpleLogger.toLevel(level)).withCause(t).log(message);
    }

}
