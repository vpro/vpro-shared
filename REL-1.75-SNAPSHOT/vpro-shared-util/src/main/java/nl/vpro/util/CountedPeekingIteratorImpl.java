package nl.vpro.util;

import java.util.Optional;

import static com.google.common.base.Preconditions.checkState;

/**
 * @author Michiel Meeuwissen
 * @since 5.1
 */
class CountedPeekingIteratorImpl<T> implements  CountedPeekingIterator<T> {
    private final CountedIterator<? extends T> iterator;
    private boolean hasPeeked;
    private T peekedElement;

    public CountedPeekingIteratorImpl(CountedIterator<? extends T> iterator) {
        this.iterator = iterator;
    }

    @Override
    public boolean hasNext() {
        return hasPeeked || iterator.hasNext();
    }

    @Override
    public T next() {
        if (!hasPeeked) {
            return iterator.next();
        }
        T result = peekedElement;
        hasPeeked = false;
        peekedElement = null;
        return result;
    }

    @Override
    public void remove() {
        checkState(!hasPeeked, "Can't remove after you've peeked at next");
        iterator.remove();
    }

    @Override
    public T peek() {
        if (!hasPeeked) {
            peekedElement = iterator.next();
            hasPeeked = true;
        }
        return peekedElement;
    }


    @Override
    public Optional<Long> getSize() {
        return iterator.getSize();

    }

    @Override
    public Long getCount() {
        return iterator.getCount();

    }

    @Override
    public Optional<Long> getTotalSize() {
        return iterator.getTotalSize();
    }



    @Override
    public void close() throws Exception {
        iterator.close();

    }

    @Override
    public CountedPeekingIterator<T> peeking() {
        return this;
    }
};

