package nl.vpro.util;

import lombok.Singular;
import lombok.extern.slf4j.Slf4j;

import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;

import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;;

/**
 * An iterator implementing offset and max, for another iterator.
 *
 * @author Michiel Meeuwissen
 * @since 3.1
 */
@Slf4j
public class MaxOffsetIterator<T> implements CloseableIterator<T> {


    private final Iterator<T> wrapped;

    private final long offsetmax;

    private final long offset;

    private final boolean countNulls;

    private long count = 0;

    private Boolean hasNext = null;

    private T next;

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
        this(wrapped, max, offset, countNulls, null);
    }

    @lombok.Builder(builderClassName = "Builder")
    private MaxOffsetIterator(
        @NonNull Iterator<T> wrapped,
        @Nullable Number max,
        @Nullable Number offset, boolean countNulls,
        @Nullable @Singular  List<Runnable> callbacks) {
        //noinspection ConstantConditions
        if (wrapped == null) {
            throw new IllegalArgumentException("Cannot wrap null");
        }
        this.wrapped = wrapped;
        this.offset = offset == null ? 0L : offset.longValue();
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

    }

    public MaxOffsetIterator<T> callBack(Runnable run) {
        callback = run;
        return this;
    }

    public MaxOffsetIterator<T> autoClose(AutoCloseable... closeables) {
        callback = () -> {
            for (AutoCloseable closeable : closeables) {
                try {
                    closeable.close();
                } catch (Exception e) {
                    log.error(e.getMessage(), e);
                }
            }

        };
        return this;
    }
    public MaxOffsetIterator<T> autoClose() {
        final Runnable prev = callback;
        callback = new Runnable() {
            @Override
            public void run() {
                try {
                    if (prev != null) {
                        prev.run();
                    }
                } finally {
                    if (wrapped instanceof AutoCloseable) {
                        try {
                            ((AutoCloseable) wrapped).close();
                        }catch(Exception e){
                            log.error(e.getMessage(), e);
                        }
                    }
                }
            }
        };
        return this;
    }

    @Override
    public boolean hasNext() {
        findNext();
        return hasNext;
    }

    @Override
    public T next() {
        findNext();
        if(!hasNext) {
            throw new NoSuchElementException();
        }
        hasNext = null;
        if (exception != null) {
            throw exception;
        }
        return next;
    }

    protected void findNext() {
        if(hasNext == null) {
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
    }


    @Override
    public void remove() {
        wrapped.remove();
    }

    @Override
    public void close() throws Exception {
        if (wrapped instanceof AutoCloseable) {
            ((AutoCloseable) wrapped).close();
        }
    }
}
