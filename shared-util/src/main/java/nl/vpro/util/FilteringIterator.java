package nl.vpro.util;

import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.function.Function;
import java.util.function.IntConsumer;
import java.util.function.LongConsumer;

import com.google.common.base.Predicate;

/**
 * @author Michiel Meeuwissen
 * @since 1.3
 */
public class FilteringIterator<T> implements Iterator<T> {

    private final Iterator<? extends T> wrapped;

    private final Predicate<? super T> filter;

    private final KeepAlive keepAlive;

    private T next;

    private Boolean hasNext = null;

    private long countForKeepAlive = 0;
    private long totalCountForKeepAlive = 0;

    public FilteringIterator(
            Iterator<? extends T> wrapped,
            Predicate<? super T> filter) {
        this(wrapped, filter, new KeepAlive(Long.MAX_VALUE, value -> {}));
    }

    public FilteringIterator(
            Iterator<? extends T> wrapped,
            Predicate<? super T> filter,
            KeepAlive keepAlive) {
        this.wrapped = wrapped;
        if (wrapped == null) throw new IllegalArgumentException();
        this.filter = filter;
        this.keepAlive = keepAlive;
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
                totalCountForKeepAlive++;
                boolean inFilter = inFilter(next);
                if (inFilter) {
                    countForKeepAlive++;
                }
                if (totalCountForKeepAlive >= keepAlive.count) {
                    keepAlive.callback.accept(countForKeepAlive);
                    countForKeepAlive = 0;
                    totalCountForKeepAlive = 0;
                }
                if(inFilter) {
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
    public static class KeepAlive {
        private final long count;
        private final LongConsumer callback;

        public KeepAlive(long count, LongConsumer callback) {
            this.count = count;
            this.callback = callback;
        }
        public static KeepAlive of(LongConsumer callback) {
            return of(100, callback);
        }

        public static KeepAlive of(long c, LongConsumer callback) {
            return new KeepAlive(c, callback);
        }
    }
}
