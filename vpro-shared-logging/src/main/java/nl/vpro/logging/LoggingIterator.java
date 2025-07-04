package nl.vpro.logging;


import java.util.Iterator;

import org.slf4j.Logger;
import org.slf4j.event.Level;
import org.slf4j.spi.LoggingEventBuilder;


/**
 * An iterator that wraps another one, but adds some logging.
 * <p>
 * Once in every 'interval' an element is logged.
 *
 * @author Michiel Meeuwissen
 * @since 3.1
 */
public class LoggingIterator<T> implements Iterator<T> {

    private final LoggingEventBuilder levelLogger;
    private long count = 0;
    private final int interval;
    private final Iterator<T> wrapped;

    @lombok.Builder
    public LoggingIterator(Iterator<T> wrapped, Logger logger, Level level, int interval) {
        this.wrapped = wrapped;
        Logger wrapper = new LoggerWrapper(logger, logger.getName());
        this.levelLogger = logger.atLevel(level);
        this.interval = interval;
    }

    public LoggingIterator(Iterator<T> wrapped, Logger logger, int interval) {
        this(wrapped, logger, Level.INFO, interval);
    }

    @Override
    public boolean hasNext() {
        return wrapped.hasNext();
    }

    @Override
    public T next() {
        T next = wrapped.next();
        if (++count % interval == 0 || ! wrapped.hasNext()) {
            levelLogger.log( "{}: {}", count, next);
        }
        return next;

    }

    @Override
    public void remove() {
        wrapped.remove();
    }
}
