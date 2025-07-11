package nl.vpro.util;

import lombok.extern.slf4j.Slf4j;

import java.util.Iterator;
import java.util.List;
import java.util.function.Function;

import org.meeuw.functional.Unwrappable;


/**
 * Wraps an iterator, to add zero or more elements at the start of it.
 * @author Michiel Meeuwissen
 * @since 1.72
 */
@Slf4j
public class HeadAdder<T> implements Iterator<T>, Unwrappable<Iterator<T>> {

    private final Function<T, T>[] adder;

    private final Iterator<T> wrapped;

    private int adderCount = 0;
    private T nextFromAdder;
    private Boolean adderHasNext;
    private Boolean hasFirst;
    private T first = null;


    @SuppressWarnings("unchecked")
    @lombok.Builder(builderClassName = "Builder")
    private HeadAdder(Iterator<T> wrapped, final boolean onlyIfEmpty, final boolean onlyIfNotEmpty, @lombok.Singular  List<Function<T, T>> adders) {
        this.wrapped = wrapped;
        if (onlyIfEmpty && onlyIfNotEmpty) {
            throw new IllegalArgumentException("Cant specify both onlyIfEmpty and onlyIfNotEmpty");
        }
        hasFirst = wrapped.hasNext();
        this.adder = adders.toArray(new Function[0]);
        if (adder.length > 0) {
            if (onlyIfEmpty) {
                adderHasNext = ! hasFirst;
            } else if (onlyIfNotEmpty) {
                adderHasNext = hasFirst;
            } else {
                adderHasNext = true;
            }
            first = hasFirst ? wrapped.next() : null;
        } else {
            adderHasNext = false;
        }

    }

    @Override
    public boolean hasNext() {
        findNextFromAdder();
        return adderHasNext || first != null || wrapped.hasNext();

    }

    @Override
    public T next() {
        findNextFromAdder();
        if (adderHasNext) {
            adderCount++;
            return nextFromAdder;
        }
        if (hasFirst != null) {
            hasFirst = null;
            T result = first;
            first = null;
            return result;
        }
        return wrapped.next();
    }

    @Override
    public Iterator<T> unwrap() {
        return wrapped;
    }

    private void findNextFromAdder() {
        while(adderHasNext) {
            try {
                nextFromAdder = adder[adderCount].apply(first);
                return;
            } catch (Exception e) {
                adderCount++;
                adderHasNext = adderCount < adder.length;
            }
        }
    }

    @Override
    public void remove() {
        throw new UnsupportedOperationException();
    }
}
