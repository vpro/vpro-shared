package nl.vpro.util;

import java.util.Iterator;
import java.util.Optional;

/**
 * @author Michiel Meeuwissen
 * @since 1.3
 */
public  abstract class WrappedIterator<T, S> implements CountedIterator<S> {

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


    @Override
    public Optional<Long> getSize() {
        if (wrapped instanceof CountedIterator) {
            return ((CountedIterator) wrapped).getSize();
        }
        return Optional.empty();

    }

}
