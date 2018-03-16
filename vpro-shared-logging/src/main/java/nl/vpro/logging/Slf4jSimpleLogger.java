package nl.vpro.logging;

import org.slf4j.Logger;
import org.slf4j.event.Level;

/**
 * @author Michiel Meeuwissen
 * @since ...
 */
public class Slf4jSimpleLogger implements SimpleLogger {

    private final Logger logger;

    public Slf4jSimpleLogger(Logger logger) {
        this.logger = logger;
    }

    @Override
    public void accept(Level level, String message, Throwable t) {
          switch (level) {
            case TRACE:
                logger.trace(message, t);
                break;
            case DEBUG:
                logger.debug(message, t);
                break;
            case INFO:
                logger.info(message, t);
                break;
            case WARN:
                logger.warn(message, t);
                break;
            default:
            case ERROR:
                logger.error(message, t);
                break;
        }
    }
}
