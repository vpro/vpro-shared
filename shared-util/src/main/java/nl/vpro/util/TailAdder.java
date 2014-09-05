package nl.vpro.util;

import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.concurrent.Callable;

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
        return wrapped.hasNext() ||
                ((!onlyIfEmpty || wrapcount == 0) && adder.length > addercount);
    }

    @Override
    public T next() {
        if (wrapped.hasNext()) {
            wrapcount++;
            return wrapped.next();
        }
        if (addercount < adder.length) {
            try {
                return adder[addercount++].call();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        } else {
            throw new NoSuchElementException();
        }
    }

    @Override
    public void remove() {
        throw new UnsupportedOperationException();
    }
}
