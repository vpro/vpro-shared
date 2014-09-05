package nl.vpro.util;

import java.util.Iterator;
import java.util.NoSuchElementException;

import com.google.common.base.Predicate;

/**
 * @author Michiel Meeuwissen
 * @since 1.3
 */
public class FilteringIterator<T> implements Iterator<T> {

    private final Iterator<? extends T> wrapped;

    private final Predicate<? super T> filter;

    private T next;

    private Boolean hasNext = null;

    public FilteringIterator(Iterator<? extends T> wrapped, Predicate<? super T> filter) {
        this.wrapped = wrapped;
        if (wrapped == null) throw new IllegalArgumentException();
        this.filter = filter;
    }

    @Override
    public boolean hasNext() {
        findNext();
        return hasNext;
    }

    @Override
    public T next() {
        findNext();
        if(! hasNext) {
            throw new NoSuchElementException();
        }
        hasNext = null;
        return next;
    }

    @Override
    public void remove() {
        wrapped.remove();
    }

    private void findNext() {
        if(hasNext == null) {
            while(wrapped.hasNext()) {
                next = wrapped.next();
                if(inFilter(next)) {
                    hasNext = true;
                    return;
                }
            }
            hasNext = false;
        }

    }

    private boolean inFilter(T object) {
        return filter == null || filter.apply(object);
    }
}
