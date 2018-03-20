package nl.vpro.logging.simple;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import javax.annotation.Nonnull;

import org.slf4j.event.Level;

/**
 * Wraps all log events to zero or more other instances of {@link SimpleLogger}
 *
 * @author Michiel Meeuwissen
 * @since 1.76
 */
public class ChainedSimpleLogger implements SimpleLogger, Iterable<SimpleLogger> {

    private final List<SimpleLogger> list = new ArrayList<>();


    public ChainedSimpleLogger(SimpleLogger... wrapped) {
        Collections.addAll(list, wrapped);
    }


    @Override
    public void accept(Level level, String message, Throwable t) {
        for (SimpleLogger logger : list) {
            logger.accept(level, message, t);;
        }
    }

    @Override
    @Nonnull
    public Iterator<SimpleLogger> iterator() {
        return list.iterator();

    }
}
