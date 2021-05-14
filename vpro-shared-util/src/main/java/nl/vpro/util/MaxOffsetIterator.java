package nl.vpro.util;

import lombok.Getter;
import lombok.Singular;
import lombok.extern.slf4j.Slf4j;

import java.util.*;

import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

import com.google.common.collect.PeekingIterator;

/**
 * An iterator implementing offset and max, for another iterator.
 *
 * @author Michiel Meeuwissen
 * @since 3.1
 */
@Slf4j
public class MaxOffsetIterator<T> implements CloseablePeekingIterator<T> {

    protected final CloseableIterator<T> wrapped;

    protected PeekingIterator<T> peekingWrapped;

    /**
     * The maximal value of count. I.e. offset + max;
     */
    protected final long offsetmax;

    final long max;


    @Getter
    private final long offset;

    private final boolean countNulls;

    /**
     * The count of the next element. First value will be the supplied value of offset.
     */
    protected long count = 0;

    private Boolean hasNext = null;

    protected T next;

    private RuntimeException exception;

    private Runnable callback;

    public MaxOffsetIterator(Iterator<T> wrapped, Number max, boolean countNulls) {
        this(wrapped, max, 0L, countNulls);
    }

    public MaxOffsetIterator(Iterator<T> wrapped, Number max) {
        this(wrapped, max, 0L, true);
    }

    public MaxOffsetIterator(Iterator<T> wrapped, Number max, Number offset) {
        this(wrapped, max, offset, true);
    }

    public MaxOffsetIterator(Iterator<T> wrapped, Number max, Number offset, boolean countNulls) {
        this(wrapped, max, offset, countNulls, null, false);
    }

    @lombok.Builder(builderClassName = "Builder")
    protected MaxOffsetIterator(
        @NonNull Iterator<T> wrapped,
        @Nullable Number max,
        @Nullable Number offset,
        boolean countNulls,
        @Nullable @Singular  List<Runnable> callbacks,
        boolean autoClose) {
        //noinspection ConstantConditions
        if (wrapped == null) {
            throw new IllegalArgumentException("Cannot wrap null");
        }
        this.wrapped = CloseableIterator.of(wrapped);
        this.offset = offset == null ? 0L : offset.longValue();
        this.max = max == null ? Long.MAX_VALUE : max.longValue();
        this.offsetmax = max == null ? Long.MAX_VALUE : max.longValue() + this.offset;
        this.countNulls = countNulls;
        this.callback = () -> {
            if (callbacks != null) {
                for (Runnable r : callbacks) {
                    try {
                        r.run();
                    } catch (Exception e) {
                        log.error(e.getMessage(), e);
                    }
                }
            }
        };
        if (autoClose) {
            autoClose();
        }

    }

    public MaxOffsetIterator<T> callBack(Runnable run) {
        callback = run;
        return this;
    }

    public MaxOffsetIterator<T> autoClose(AutoCloseable... closeables) {
        final Runnable prev = callback;
        callback = () -> {
            try {
                if (prev != null) {
                    prev.run();
                }
            } finally {
                for (AutoCloseable closeable : closeables) {
                    try {
                        closeable.close();
                    } catch (Exception e) {
                        log.error(e.getMessage(), e);
                    }
                }
            }
        };
        return this;
    }

    public MaxOffsetIterator<T> autoClose() {
        final Runnable prev = callback;
        callback = () -> {
            try {
                if (prev != null) {
                    prev.run();
                }
            } finally {
                try {
                    wrapped.close();
                } catch(Exception e){
                    log.error(e.getMessage(), e);
                }
            }
        };
        return this;
    }

    @Override
    public boolean hasNext() {
        return findNext();
    }

    @Override
    public T peek() {
        if (!findNext()) {
            throw new NoSuchElementException();
        }
        if (exception != null) {
            throw exception;
        }
        return next;
    }

    @Override
    public T next() {
        if (!findNext()) {
            throw new NoSuchElementException();
        }
        hasNext = null;
        if (exception != null) {
            throw exception;
        }
        return next;
    }

    protected boolean findNext() {
        if (hasNext == null) {
            hasNext = false;

            while(count < offset && wrapped.hasNext()) {
                T n;
                try {
                    n = wrapped.next();
                } catch(RuntimeException runtimeException) {
                    n = null;
                }
                if (countNulls || n != null) {
                    count++;
                }
            }

            if(count < offsetmax && wrapped.hasNext()) {
                try {
                    exception = null;
                    next = wrapped.next();
                } catch (RuntimeException e) {
                    exception = e;
                    next = null;

                }
                if (countNulls || next != null) {
                    count++;
                }
                hasNext = true;
            }

            if(!hasNext && callback != null) {
                callback.run();
            }
        }
        return hasNext;
    }


    @Override
    public void remove() {
        wrapped.remove();
    }

    @Override
    public void close() throws Exception {
        wrapped.close();
    }

    @Override
    public String toString() {
        return wrapped + "[" + offset + "," + (max < Long.MAX_VALUE ? max : "") + "]";
    }

    /**
     * Access to the (peeking) wrapped iterator.
     * This may be used to look 'beyond' max, to check what would have been the next one.
     */
    public PeekingIterator<T> peekingWrapped() {
        if (peekingWrapped == null) {
            peekingWrapped = wrapped.peeking();
        }
        return peekingWrapped;
    }

    public static <T> CountedMaxOffsetIterator.Builder<T> countedBuilder() {
        return CountedMaxOffsetIterator._countedBuilder();
    }
}
