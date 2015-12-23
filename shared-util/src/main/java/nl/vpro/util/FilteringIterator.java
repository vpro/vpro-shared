package nl.vpro.util;

import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.function.LongConsumer;
import java.util.function.Predicate;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * @author Michiel Meeuwissen
 * @since 1.3
 */
public class FilteringIterator<T> implements CloseableIterator<T> {

    protected static final Logger LOG = LoggerFactory.getLogger(FilteringIterator.class);


    private final Iterator<? extends T> wrapped;

    private final Predicate<? super T> filter;

    private final KeepAlive keepAlive;

    private T next;

    private RuntimeException exception;

    private Boolean hasNext = null;

    private long countForKeepAlive = 0;
    private long totalCountForKeepAlive = 0;

    public FilteringIterator(
            Iterator<? extends T> wrapped,
            Predicate<? super T> filter) {
        this(wrapped, filter, noKeepAlive());
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
        if (exception != null) {
            throw exception;
        }
        return next;
    }

    @Override
    public void remove() {
        wrapped.remove();
    }

    private void findNext() {
        while(hasNext == null) {
            exception = null;
            try {
                while (wrapped.hasNext()) {
                    next = wrapped.next();
                    totalCountForKeepAlive++;
                    boolean inFilter = inFilter(next);
                    if (inFilter) {
                        countForKeepAlive++;
                    }
                    if (totalCountForKeepAlive >= keepAlive.count) {
                        Boolean mustBreak = keepAlive.callback.apply(countForKeepAlive);
                        if (mustBreak) {
                            hasNext = false;
                            return;
                        }
                        countForKeepAlive = 0;
                        totalCountForKeepAlive = 0;
                    }
                    if (inFilter) {
                        hasNext = true;
                        return;
                    }
                }
                hasNext = false;
            } catch (RuntimeException e) {
                LOG.error(e.getClass().getName() + " " + e.getMessage());
                exception = e;
                hasNext = true;
                next = null;
            }
        }

    }


    private boolean inFilter(T object) {
        return filter == null || filter.test(object);
    }
    public static KeepAlive noKeepAlive() {
        return keepAliveWithoutBreaks(Long.MAX_VALUE, (long value) -> {});
    }

    public static KeepAlive keepAliveWithoutBreaks(LongConsumer callback) {
        return keepAliveWithoutBreaks(100, callback);
    }

    public static KeepAlive keepAliveWithoutBreaks(long c, LongConsumer callback) {
        return new KeepAlive(c, aLong -> {
            callback.accept(aLong);
            return false;
        });
    }

    public static KeepAlive keepAlive(Function<Long, Boolean> callback) {
        return keepAlive(100, callback);
    }

    public static KeepAlive keepAliveChars(Function<Character, Boolean> callback) {
        return keepAliveChars(100, callback);
    }

    /**
     * This translates the 'numberOfRecords' to a character to write if no records are outputted by the iterated since the last call to the callback.
     * The callback is called with the character. The idea is to write it simply to the stream, to keep it 'alive.
     * The use case is in MediaRestServiceImpl#iterate
     * @param c
     * @param callback
     * @return
     */
    public static KeepAlive keepAliveChars(long c, Function<Character, Boolean> callback) {
        final char[] writeChar = new char[]{
            '\n'
        };
        final AtomicInteger numberOfKeepAlives = new AtomicInteger(0);
        return keepAlive(c, numberOfRecords -> {
            if (numberOfRecords == 0) {
                Boolean result = callback.apply(writeChar[0]);
                if (numberOfKeepAlives.incrementAndGet() > 50) {
                    writeChar[0] = '\n';
                    numberOfKeepAlives.set(0);
                } else {
                    writeChar[0] = ' ';
                }
                return result;
            } else {
                return false;
            }
        });
    }

    public static KeepAlive keepAlive(long c, Function<Long, Boolean> callback) {
        return new KeepAlive(c, callback);
    }

    @Override
    public void close() throws Exception {
        if (wrapped instanceof AutoCloseable) {
            ((AutoCloseable) wrapped).close();
        }
    }


    public static class KeepAlive {
        private final long count;
        private final Function<Long, Boolean> callback;

        /**
         *
         * @param count The callback function is called every so much records
         * @param callback This function is called then. The argument is the number
         *                 of outputted records since the previous call (which weren't filtered out)
         *                 The call function can return a boolean to indicate whether the complete iteration must be 'break'ed.
         *
         */
        public KeepAlive(long count, Function<Long, Boolean> callback) {
            this.count = count;
            this.callback = callback;

        }
    }
}
