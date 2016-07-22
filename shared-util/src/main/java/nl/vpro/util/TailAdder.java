package nl.vpro.util;

import java.util.Arrays;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.concurrent.Callable;
import java.util.function.Function;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * @author Michiel Meeuwissen
 * @since 1.17
 */
public class TailAdder<T> implements Iterator<T> {

    private static final Logger LOG = LoggerFactory.getLogger(TailAdder.class);

    private final boolean onlyIfEmpty;

    private final Function<T, T>[] adder;

    private final Iterator<T> wrapped;

    int wrapcount = 0;
    int addercount = 0;
    T nextFromAdder;
    Boolean adderHasNext = null;
    T last = null;


    public static <T> TailAdder withFunctions(Iterator<T> wrapped, Function<T, T>... adder) {
        return new TailAdder(wrapped, false, (Function<T, T>[]) adder);
    }

    @SafeVarargs
    private TailAdder(Iterator<T> wrapped, boolean onlyIfEmpty, Function<T, T>... adder) {
        this.wrapped = wrapped;
        this.onlyIfEmpty = onlyIfEmpty;
        this.adder = adder;
    }

    private TailAdder(Iterator<T> wrapped, Function<T, T>... adder) {
        this(wrapped, false, adder);
    }


    @SuppressWarnings("unchecked")
    @SafeVarargs
    @Deprecated
    public TailAdder(Iterator<T> wrapped, boolean onlyIfEmpty, Callable<T>... adder) {
        this(wrapped, onlyIfEmpty, (Function[]) Arrays.stream(adder).map(c -> (Function<T, T>) last1 -> {
            try {
                return c.call();
            } catch (RuntimeException e) {
                throw e;
            } catch (Exception e) {
                throw new RuntimeException(e);
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
                    try {
                        nextFromAdder = adder[addercount++].apply(getLast());
                        adderHasNext = true;
                        break;
                    } catch (NoSuchElementException nse) {
                        // ignore
                    } catch (Exception e) {
                        LOG.warn(e.getMessage());

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
