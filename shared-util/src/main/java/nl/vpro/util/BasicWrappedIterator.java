package nl.vpro.util;

import java.util.Iterator;

/**
 * @author Michiel Meeuwissen
 * @since 0.31
 */
public class BasicWrappedIterator<T> extends WrappedIterator<T, T> {

    public BasicWrappedIterator(Iterator<T> wrapped) {
        super(wrapped);
    }

    @Override
    public T next() {
        return wrapped.next();

    }
}
