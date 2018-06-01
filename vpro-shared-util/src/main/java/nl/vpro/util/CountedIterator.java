package nl.vpro.util;

import java.util.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Stream;

/**
 * An iterator that is also aware of the current position {@link #getCount()}, and optionally of the total size {@link #getSize()}, and also optionally of a 'total' size (in case this iterator presents some sub-collection {@link #getTotalSize()}.
 *
 * @author Michiel Meeuwissen
 * @since 0.31
 */
public interface CountedIterator<T> extends Iterator<T>, CloseableIterator<T> {

    static <S> CountedIterator<S> of(Collection<S> wrapped) {
        return new BasicWrappedIterator<>(wrapped);
    }
    static <S> CountedPeekingIterator<S> peeking(CountedIterator<S> wrapped){
        return wrapped == null ? null : wrapped.peeking();
    }

    static <S> CountedIterator<S> of(Stream<S> wrapped) {
        Spliterator<S> spliterator = wrapped.spliterator();
        return new BasicWrappedIterator<>(spliterator.getExactSizeIfKnown(), Spliterators.iterator(spliterator));
    }

    static <C> CountedIterator<C> of(Long size, Iterator<C> wrapped) {
        return new BasicWrappedIterator<>(size, wrapped);
    }

    static <C> CountedIterator<C> of(AtomicLong size, Iterator<C> wrapped) {
        return new BasicWrappedIterator<>(size, wrapped);
    }


    /**
     * The size, if known, of the collection this iterator is representing
     */
    Optional<Long> getSize();

    /**
     * The current position.
     */
    default Long getCount() {
        throw new UnsupportedOperationException();
    }

    /**
     * If the iterator is in some way restricted you may also want to report a total size, representing the unrestricted size.
     *
     * The default implementation is {@link #getSize()}.
     */
    default Optional<Long> getTotalSize() {
        return getSize();
    }

    /**
     * Returns this iterator as {@link Spliterator}.
     */

    default Spliterator<T> spliterator() {
        Optional<Long> size = getSize();
        return size
            .map(s -> Spliterators.spliterator(this, s, Spliterator.SIZED))
            .orElseGet(() -> Spliterators.spliteratorUnknownSize(this, 0));
    }

    @Override
    default void close() throws Exception {

    }

    /**
     * If you need a guava {@link com.google.common.collect.PeekingIterator}, this will make you one.
     */
    default CountedPeekingIterator<T> peeking() {
        return new CountedPeekingIteratorImpl<>(this);
    }
}

