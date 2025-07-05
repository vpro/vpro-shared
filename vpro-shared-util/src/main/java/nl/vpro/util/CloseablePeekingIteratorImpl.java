package nl.vpro.util;

import java.util.Iterator;

import org.meeuw.functional.Unwrappable;

import static com.google.common.base.Preconditions.checkState;

/**
 *
 * @author Michiel Meeuwissen
 * @since 5.1
 */
class CloseablePeekingIteratorImpl<T> implements CloseablePeekingIterator<T>, Unwrappable<CloseableIterator<? extends T>> {
    protected final CloseableIterator<? extends T> iterator;
    private boolean hasPeeked;
    private T peekedElement;

    public CloseablePeekingIteratorImpl(Iterator<? extends T> iterator) {
        this.iterator = CloseableIterator.of(iterator);
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
    public void close() throws Exception {
        iterator.close();
    }

    @Override
    public CloseableIterator<? extends T> unwrap() {
        return iterator;
    }
}

