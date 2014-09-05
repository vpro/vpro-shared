package nl.vpro.util;

import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.concurrent.Callable;

import com.google.common.base.Optional;

/**
 * @author Michiel Meeuwissen
 * @since 1.17
 */
public class TailAdder<T> implements Iterator<T> {

    private final boolean onlyIfEmpty;

    private final Callable<T>[] adder;

    private final Iterator<T> wrapped;

    int wrapcount = 0;
    int addercount = 0;
    T nextFromAdder;
    Boolean adderHasNext = null;

    @SafeVarargs
    public TailAdder(Iterator<T> wrapped, boolean onlyIfEmpty, Callable<T>... adder) {
        this.wrapped = wrapped;
        this.onlyIfEmpty = onlyIfEmpty;
        this.adder = adder;
    }

    public TailAdder(Iterator<T> wrapped, Callable<T> adder) {
        this(wrapped, false, adder);
    }


    @Override
    public boolean hasNext() {
        if (wrapped.hasNext()) {
            return true;
        }
        findNext();
        return adderHasNext;
    }

    @Override
    public T next() {
        if (wrapped.hasNext()) {
            wrapcount++;
            return wrapped.next();
        }
        findNext();
        if (! adderHasNext) {
            throw new NoSuchElementException();
        }
        adderHasNext = null;
        return nextFromAdder;
    }

    private void findNext() {
        if (adderHasNext == null) {
            adderHasNext = false;
            if (wrapcount == 0 || ! onlyIfEmpty) {
                while (addercount < adder.length) {
                    try {
                        nextFromAdder = adder[addercount++].call();
                        adderHasNext = true;
                        break;
                    } catch (Exception e) {
                        // skip this
                    }
                }
            }
        }
    }

    @Override
    public void remove() {
        throw new UnsupportedOperationException();
    }
}
