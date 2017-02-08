package nl.vpro.util;

import java.util.*;

/**
 * @author Michiel Meeuwissen
 * @since 0.31
 */
public interface CountedIterator<T> extends Iterator<T>, CloseableIterator<T> {

    static <S> CountedIterator<S> of(Collection<S> wrapped) {
        return new BasicWrappedIterator<S>(wrapped);
    }
    static <S> CountedPeekingIterator<S> peeking(CountedIterator<S> wrapped){
        return wrapped == null ? null : wrapped.peeking();

    }

    Optional<Long> getSize();

    default Long getCount() {
        throw new UnsupportedOperationException();
    }

    /**
     * If the iterator is in some way restricted you may also want to report a total size, representing the unrestricted size.
     */
    default Optional<Long> getTotalSize() {
        return getSize();
    }


    default Spliterator<T> spliterator() {
        Optional<Long> size = getSize();
        if (size.isPresent()) {
            return Spliterators.spliterator(this, getSize().get(), Spliterator.SIZED);
        } else {
            return Spliterators.spliteratorUnknownSize(this, 0);
        }
    }

    @Override
    default void close() throws Exception {

    }

    default CountedPeekingIterator<T> peeking() {
        return new CountedPeekingIteratorImpl<T>(this);
    }
}

