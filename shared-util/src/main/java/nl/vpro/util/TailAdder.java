package nl.vpro.util;

import java.util.Arrays;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.function.Function;


/**
 * @author Michiel Meeuwissen
 * @since 1.17
 */
public class TailAdder<T> implements Iterator<T> {

    private final boolean onlyIfEmpty;

    private final Function<T, Optional<T>>[] adder;

    private final Iterator<T> wrapped;

    int wrapcount = 0;
    int addercount = 0;
    T nextFromAdder;
    Boolean adderHasNext = null;
    T last = null;

    @SafeVarargs
    public TailAdder(Iterator<T> wrapped, boolean onlyIfEmpty, Function<T, Optional<T>>... adder) {
        this.wrapped = wrapped;
        this.onlyIfEmpty = onlyIfEmpty;
        this.adder = adder;
    }

    public TailAdder(Iterator<T> wrapped, Function<T, Optional<T>>... adder) {
        this(wrapped, false, adder);
    }


    @SuppressWarnings("unchecked")
    @SafeVarargs
    @Deprecated
    public TailAdder(Iterator<T> wrapped, boolean onlyIfEmpty, Callable<T>... adder) {
        this(wrapped, onlyIfEmpty, Arrays.stream(adder).map(c -> {
            try {
                return Optional.ofNullable(c.call());
            } catch (Exception e) {
                return Optional.empty();
            }
        }).toArray(Function[]::new));
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
            T result = wrapped.next();
            last = result;
            return result;
        }
        findNext();
        if (! adderHasNext) {
            throw new NoSuchElementException();
        }
        adderHasNext = null;
        return nextFromAdder;
    }

    protected T getLast() {
        return last;
    }

    private void findNext() {
        if (adderHasNext == null) {
            adderHasNext = false;
            if (wrapcount == 0 || ! onlyIfEmpty) {
                while (addercount < adder.length) {
                    Optional<T> next = adder[addercount++].apply(getLast());
                    if (next.isPresent()) {
                        nextFromAdder = next.get();
                        adderHasNext = true;
                        break;
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
