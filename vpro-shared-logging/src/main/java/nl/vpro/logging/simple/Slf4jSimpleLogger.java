package nl.vpro.logging.simple;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import nl.vpro.logging.Slf4jHelper;

/**
 * Wraps an SLF4J {@link Logger}

 * @author Michiel Meeuwissen
 * @since 1.76
 */
public class Slf4jSimpleLogger implements SimpleLogger {

    private final Logger logger;

    private final Level threshold;

    public Slf4jSimpleLogger(Logger logger) {
        this(logger, Level.TRACE);
    }

    protected Slf4jSimpleLogger(Logger logger, Level threshold) {
        this.logger = logger;
        this.threshold = threshold;
    }

    public Slf4jSimpleLogger(Class<?> clazz) {
        this(LoggerFactory.getLogger(clazz));
    }

    public static Slf4jSimpleLogger of(Logger logger) {
        return new Slf4jSimpleLogger(logger);
    }

    /**
     * Like {@link #of(Logger)}, but with this name it's nicer of static imports
     */
    public static Slf4jSimpleLogger slf4j(Logger logger) {
        return of(logger);
    }
    public static Slf4jSimpleLogger of(String category) {
        return of(LoggerFactory.getLogger(category));
    }

    @Override
    public boolean isEnabled(Level level) {
        return level.toInt() >= threshold.toInt() && Slf4jHelper.isEnabled(logger, org.slf4j.event.Level.valueOf(level.name()));
    }

    @Override
    public String getName() {
        return logger.getName();

    }

    @Override
    public void accept(Level level, CharSequence message, Throwable t) {
        if (isEnabled(level)) {
            Slf4jHelper.log(logger,
                org.slf4j.event.Level.valueOf(level.name()),
                message == null ? null : message.toString(), t);
        }
    }

    public Slf4jSimpleLogger withThreshold(Level level) {
        return new Slf4jSimpleLogger(logger, level);
    }


    @Override
    public String toString() {
        return "slf4j:" + logger.getName();
    }

    public static SimpleLogger chain(SimpleLogger start, Logger... logger) {
        SimpleLogger[] array = new SimpleLogger[logger.length + 1];
        array[0] = start;
        for (int i = 1; i <= logger.length; i++) {
            array[i] = new Slf4jSimpleLogger(logger[i - 1]);
        }
        return new ChainedSimpleLogger(array);
    }

}
