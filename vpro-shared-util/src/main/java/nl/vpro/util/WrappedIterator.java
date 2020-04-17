package nl.vpro.util;

import java.util.Iterator;
import java.util.Optional;

import org.checkerframework.checker.nullness.qual.NonNull;

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


    @SuppressWarnings("unchecked")
    @Override
    public @NonNull Optional<Long> getSize() {
        if (wrapped instanceof CountedIterator) {
            return ((CountedIterator) wrapped).getSize();
        }
        return Optional.empty();

    }

    @SuppressWarnings("unchecked")
    @Override
    public Optional<Long> getTotalSize() {
        if (wrapped instanceof CountedIterator) {
            return ((CountedIterator) wrapped).getTotalSize();
        }
        return Optional.empty();

    }

    @Override
    public void close() throws Exception {
        if (wrapped instanceof AutoCloseable) {
            ((AutoCloseable) wrapped).close();
        }
    }
}
