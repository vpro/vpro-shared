package nl.vpro.util;

import java.util.Optional;

import org.checkerframework.checker.nullness.qual.NonNull;


/**
 *
 * @author Michiel Meeuwissen
 * @since 5.1
 */
@SuppressWarnings("rawtypes")
class CountedPeekingIteratorImpl<T> extends CloseablePeekingIteratorImpl<T> implements  CountedPeekingIterator<T> {

    private final CountedIterator<? extends T> wrapped;

    public CountedPeekingIteratorImpl(CountedIterator<? extends T> iterator) {
        super(iterator);
        this.wrapped = iterator;
    }

    @Override
    @NonNull
    public Optional<Long> getSize() {
        return wrapped.getSize();
    }

    @Override
    public Long getCount() {
        return ((CountedIterator) iterator).getCount();
    }

    @Override
    @NonNull
    public Optional<Long> getTotalSize() {
        return wrapped.getTotalSize();
    }

    @Override
    public void close() throws Exception {
        iterator.close();
    }

    @Override
    public CountedPeekingIterator<T> peeking() {
        return this;
    }
}

