package nl.vpro.util;

import lombok.Builder;

import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.function.BiFunction;

/**
 * A wrapping iterator with the option to skip certain entries (based on comparing with the previous entry)
 * @author Michiel Meeuwissen
 * @since 1.68
 */
public class SkippingIterator<T> implements Iterator<T> {

    private final Iterator<T> wrapped;

    private final BiFunction<T, T, Boolean> comparator;

    private Boolean hasNext = null;

    private T next;

    @Builder
    public SkippingIterator(
        Iterator<T> wrapped,
        BiFunction<T, T, Boolean> comparator) {
        this.wrapped = wrapped;
        this.comparator = comparator == null ? Objects::equals : comparator;
    }

    public SkippingIterator(
        Iterator<T> wrapped) {
        this(wrapped, Objects::equals);
    }


    @Override
    public boolean hasNext() {
        findNext();
        return hasNext;
    }

    @Override
    public T next() {
        findNext();
        if (hasNext) {
            hasNext = null;
            return next;
        } else {
            throw new NoSuchElementException();
        }
    }

    protected void findNext() {
        if (hasNext == null) {
            hasNext = false;

            while (wrapped.hasNext()) {
                T n = wrapped.next();
                T previous = next;
                if (comparator.apply(previous, n)) {
                    continue;
                } else {
                    hasNext = true;
                    next = n;
                    break;
                }
            }
        }
    }


}
