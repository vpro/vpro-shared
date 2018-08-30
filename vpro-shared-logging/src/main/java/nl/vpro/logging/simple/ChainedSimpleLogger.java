package nl.vpro.logging.simple;

import java.util.*;

import javax.annotation.Nonnull;

import org.slf4j.event.Level;

/**
 * Wraps all log events to zero or more other instances of {@link SimpleLogger}
 *
 * @author Michiel Meeuwissen
 * @since 1.76
 */
public class ChainedSimpleLogger implements SimpleLogger, Iterable<SimpleLogger> {

    final List<SimpleLogger> list = new ArrayList<>();


    public ChainedSimpleLogger(SimpleLogger... wrapped) {
        Collections.addAll(list, wrapped);
    }


    @Override
    public void accept(Level level, CharSequence message, Throwable t) {
        for (SimpleLogger logger : list) {
            if (logger.isEnabled(level)) {
                logger.accept(level, message, t);;
            }
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    public void accept(Level level, CharSequence message) {
        for (SimpleLogger logger : list) {
            if (logger.isEnabled(level)) {
                logger.accept(level, message);;
            }
        } }

    @Override
    public boolean isEnabled(Level level){
        return list.stream().map(l -> l.isEnabled(level)).filter(b -> b).findFirst().orElse(false);
    }

    @Override
    @Nonnull
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
}
