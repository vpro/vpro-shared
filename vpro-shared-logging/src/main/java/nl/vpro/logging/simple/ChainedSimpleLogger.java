package nl.vpro.logging.simple;

import java.util.*;
import java.util.function.Predicate;

import org.checkerframework.checker.nullness.qual.NonNull;


/**
 * Wraps all log events to zero or more other instances of {@link SimpleLogger}
 *
 * @author Michiel Meeuwissen
 * @since 1.76
 */
public class ChainedSimpleLogger implements SimpleLogger, Iterable<SimpleLogger> {

    final List<SimpleLogger> list;


    public ChainedSimpleLogger(SimpleLogger... wrapped) {
        list = new ArrayList<>();
        Collections.addAll(list, wrapped);
    }

    protected ChainedSimpleLogger(List<SimpleLogger> wrapped) {
        this.list = wrapped;
    }


    @Override
    public void accept(Level level, CharSequence message, Throwable t) {
        for (SimpleLogger logger : list) {
            if (logger.isEnabled(level)) {
                logger.accept(level, message, t);
            }
        }
    }

    @Override
    public void accept(Level level, CharSequence message) {
        for (SimpleLogger logger : list) {
            if (logger.isEnabled(level)) {
                logger.accept(level, message);
            }
        }
    }

    @Override
    public boolean isEnabled(Level level) {
        return list.stream().map(l -> l.isEnabled(level)).filter(b -> b).findFirst().orElse(false);
    }

    @Override
    @NonNull
    public Iterator<SimpleLogger> iterator() {
        return list.iterator();
    }

    public static SimpleLogger of(SimpleLogger... loggers) {
        SimpleLogger[] withoutNulls = Arrays.stream(loggers).filter(Objects::nonNull).toArray(SimpleLogger[]::new);
        if (withoutNulls.length == 0) {
            return new NOPLogger();
        } else if (withoutNulls.length == 1) {
            return withoutNulls[0];
        } else {
            return new ChainedSimpleLogger(withoutNulls);
        }
    }

    @Override
    public String toString() {
        return "chained " + list;
    }

    /**
     * Creates a new {@link ChainedSimpleLogger} where the logger at index {@code i} is replaced by a truncated version.
     * @since 5.14.1
     */
    public ChainedSimpleLogger truncated(int i, Level level) {
        List<SimpleLogger> newList = new ArrayList<>();
        for (int j = 0; j < list.size(); j++) {
            SimpleLogger elogger = list.get(j);
            if (i == j) {
                elogger = elogger.truncated(level);
            }
            newList.add(elogger);
        }
        return new ChainedSimpleLogger(newList);
    }

    /**
     * Creates a new {@link ChainedSimpleLogger} where the loggers testing true are replaced by a truncated version.
     * @param predicate  The predicate to select which loggers to truncate
     * @since 5.14.1
     *
     */
    public ChainedSimpleLogger truncated(Predicate<SimpleLogger> predicate, Level level) {
        List<SimpleLogger> newList = new ArrayList<>();
        for (SimpleLogger elogger : list) {
            if (predicate.test(elogger)) {
                elogger = elogger.truncated(level);
            }
            newList.add(elogger);
        }
        return new ChainedSimpleLogger(newList);
    }

    /**
     * Creates a new {@link ChainedSimpleLogger} where the loggers of a certain class are replaced by a truncated version.
     * @since 5.14.1
     */
    public <C extends SimpleLogger> ChainedSimpleLogger truncated(Class<C> predicate, Level level) {
        return truncated(predicate::isInstance, level);
    }
}
