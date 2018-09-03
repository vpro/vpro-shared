package nl.vpro.util;

import lombok.extern.slf4j.Slf4j;

import java.util.*;
import java.util.concurrent.Callable;
import java.util.function.Function;


/**
 * Adapts an existing iterator, to add elements at the end, perhaps based on the last element.
 *
 * @author Michiel Meeuwissen
 * @since 1.17
 */
@Slf4j
public class TailAdder<T> implements CountedIterator<T> {

    private final boolean onlyIfEmpty;

    private final boolean onlyIfNotEmpty;

    private final Function<T, T>[] adder;

    private final Iterator<T> wrapped;

    int wrapcount = 0;
    int addercount = 0;
    T nextFromAdder;
    Boolean adderHasNext = null;
    T last = null;

    @SuppressWarnings("unchecked")
    @SafeVarargs
    public static <T> TailAdder<T> withFunctions(Iterator<T> wrapped, Function<T, T>... adder) {
        return new TailAdder<T>(wrapped, false, false, (Function<T, T>[]) adder);
    }

    @SafeVarargs
    private TailAdder(Iterator<T> wrapped, boolean onlyIfEmpty, boolean onlyIfNotEmpty, Function<T, T>... adder) {
        this.wrapped = wrapped;
        this.onlyIfEmpty = onlyIfEmpty;
        this.onlyIfNotEmpty = onlyIfNotEmpty;
        if (onlyIfEmpty && onlyIfNotEmpty) {
            throw new IllegalArgumentException("Cant specify both onlyIfEmpty and onlyIfNotEmpty");
        }
        this.adder = adder;
    }

    @SuppressWarnings("unchecked")
    @lombok.Builder
    private TailAdder(Iterator<T> wrapped, boolean onlyIfEmpty, boolean onlyIfNotEmpty, @lombok.Singular  List<Function<T, T>> adders) {
        this(wrapped, onlyIfEmpty, onlyIfNotEmpty, adders.toArray(new Function[adders.size()]));
    }

    @SafeVarargs
    private TailAdder(Iterator<T> wrapped, Function<T, T>... adder) {
        this(wrapped, false, false, adder);
    }


    @SuppressWarnings("unchecked")
    @SafeVarargs
    @Deprecated
    public TailAdder(Iterator<T> wrapped, boolean onlyIfEmpty, Callable<T>... adder) {
        this(wrapped, onlyIfEmpty, false, (Function[]) Arrays.stream(adder).map(c -> (Function<T, T>) last1 -> {
            try {
                return c.call();
            } catch (RuntimeException e) {
                throw e;
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }).toArray(Function[]::new));
    }



    @Deprecated
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
            boolean addTail;
            if (onlyIfNotEmpty) {
                addTail = wrapcount > 0;
            } else if (onlyIfEmpty) {
                addTail = wrapcount == 0;
            } else {
                addTail = true;
            }

            if (addTail) {
                while (addercount < adder.length) {
                    try {
                        nextFromAdder = adder[addercount++].apply(getLast());
                        adderHasNext = true;
                        break;
                    } catch (NoSuchElementException nse) {
                        // ignore
                    } catch (Exception e) {
                        log.warn(e.getMessage());

                    }

                }
            }
        }
    }

    @Override
    public void remove() {
        throw new UnsupportedOperationException();
    }

    @Override
    public Optional<Long> getSize() {
        if (wrapped instanceof CountedIterator) {
            Optional<Long> wrappedSize = ((CountedIterator) wrapped).getSize();
            if (wrappedSize.isPresent()) {
                long l = wrappedSize.get();
                if (!onlyIfEmpty || l == 0L) {
                    l++;
                }
                return Optional.of(l);
            }

        }
        return Optional.empty();
    }


    @Override
    public void close() throws Exception {
        if (wrapped instanceof AutoCloseable) {
            ((AutoCloseable) wrapped).close();
        }

    }

    @Override
    public String toString() {
        return wrapped + " + TAIL";
    }
}
