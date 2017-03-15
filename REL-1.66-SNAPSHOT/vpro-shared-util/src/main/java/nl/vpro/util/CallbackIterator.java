package nl.vpro.util;

import java.util.Iterator;
import java.util.Optional;

/**
 * @author Michiel Meeuwissen
 */
public class CallbackIterator<T> implements CountedIterator<T> {

    private final Iterator<T> wrapped;
    private Runnable callback;
    private Boolean hasNext;

    public CallbackIterator(Iterator<T> wrapped, Runnable callback) {
        this.wrapped = wrapped;
        this.callback = callback;

    }

    @Override
    public boolean hasNext() {
        findNext();
        return hasNext;
    }

    @Override
    public T next() {
        findNext();
        hasNext = null;
        T result = wrapped.next();
        findNext();
        return result;

    }

    @Override
    public void remove() {
        wrapped.remove();
    }


    protected boolean findNext() {
        if (hasNext == null) {
            hasNext = wrapped.hasNext();
            if (! hasNext && callback != null) {
                callback.run();
            }
        }
        return hasNext;

    }


    @Override
    public Optional<Long> getSize() {
        if (wrapped instanceof CountedIterator) {
            return ((CountedIterator) wrapped).getSize();
        }
        return Optional.empty();

    }
}
