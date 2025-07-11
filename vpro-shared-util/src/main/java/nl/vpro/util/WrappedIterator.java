package nl.vpro.util;

import java.util.Iterator;
import java.util.Optional;

import org.checkerframework.checker.nullness.qual.NonNull;
import org.meeuw.functional.Unwrappable;

/**
 * @author Michiel Meeuwissen
 * @since 1.3
 */
public  abstract class WrappedIterator<T, S> implements CountedIterator<S>, Unwrappable<CloseableIterator<T>> {

    protected final CloseableIterator<T> wrapped;

    public WrappedIterator(Iterator<T> wrapped) {
        this.wrapped = CloseableIterator.of(wrapped);
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


    @SuppressWarnings({"unchecked", "rawtypes"})
    @Override
    public @NonNull Optional<Long> getSize() {
        if (wrapped instanceof CountedIterator) {
            return ((CountedIterator) wrapped).getSize();
        }
        return Optional.empty();

    }

    @SuppressWarnings("rawtypes")
    @Override
    public Long getCount() {
        if (wrapped instanceof CountedIterator) {
            return ((CountedIterator) wrapped).getCount();
        }
        throw new UnsupportedOperationException();
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    @Override
    public @NonNull Optional<Long> getTotalSize() {
        if (wrapped instanceof CountedIterator) {
            return ((CountedIterator) wrapped).getTotalSize();
        }
        return Optional.empty();

    }

    @Override
    public void close() throws Exception {
        wrapped.close();
    }

    @Override
    public CloseableIterator<T> unwrap() {
        return wrapped;
    }


    @Override
    public String toString() {
        return "Counted[" + wrapped  + "]";
    }
}
