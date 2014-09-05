package nl.vpro.util;

import java.util.Iterator;

/**
 * @author Michiel Meeuwissen
 * @since 1.3
 */
public  abstract class WrappedIterator<T, S> implements Iterator<S> {

    protected final Iterator<T> wrapped;

    public WrappedIterator(Iterator<T> wrapped) {
        this.wrapped = wrapped;
    }

    @Override
    public boolean hasNext() {
        return iterator().hasNext();
    }

    @Override
    public abstract S next();

    @Override
    public void remove() {
        iterator().remove();
    }

    protected Iterator<T> iterator() {
        return wrapped;
    }

}
