package nl.vpro.util;

import uk.org.lidalia.slf4jext.Level;
import uk.org.lidalia.slf4jext.Logger;

import java.util.Iterator;


/**
 * @author Michiel Meeuwissen
 * @since 3.1
 */
public class LoggingIterator<T> extends WrappedIterator<T, T> {

	private final Logger logger;
    private final Level level;
	private long count = 0;
	private final int interval;

    public LoggingIterator(Iterator<T> wrapped, org.slf4j.Logger logger, Level level, int interval) {
		super(wrapped);
		this.logger = new Logger(logger);
        this.level = level;
		this.interval = interval;
	}

    public LoggingIterator(Iterator<T> wrapped, org.slf4j.Logger logger, int interval) {
        this(wrapped, logger, Level.INFO, interval);
    }

    @Override
	public T next() {
		T next = wrapped.next();
		if (++count % interval == 0 || ! wrapped.hasNext()) {
            logger.log(level, "{}: {}", count, next);
        }
		return next;

	}
}
