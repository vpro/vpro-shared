package nl.vpro.util;

import java.util.Iterator;
import java.util.NoSuchElementException;

/**
 * An iterator implementing offset and max, for another iterator.
 *
 * @author Michiel Meeuwissen
 * @since 3.1
 */
public class MaxOffsetIterator<T> implements Iterator<T> {

    private final Iterator<T> wrapped;

    private final long offsetmax;

    private final long offset;

    private final boolean countNulls;

    private long count = 0;

    private Boolean hasNext = null;

    private T next;

    private Runnable callback;



    public MaxOffsetIterator(Iterator<T> wrapped, Number max, boolean countNulls) {
        this(wrapped, max, 0l, countNulls);
    }

    public MaxOffsetIterator(Iterator<T> wrapped, Number max) {
        this(wrapped, max, 0l, true);
    }

    public MaxOffsetIterator(Iterator<T> wrapped, Number max, Number offset) {
        this(wrapped, max, offset, true);
    }

    public MaxOffsetIterator(Iterator<T> wrapped, Number max, Number offset, boolean countNulls) {
        this.wrapped = wrapped;
        this.offset = offset == null ? 0l : offset.longValue();
        this.offsetmax = max == null ? Long.MAX_VALUE : max.longValue() + this.offset;
        this.countNulls = countNulls;
    }

    public MaxOffsetIterator<T> callBack(Runnable run) {
        callback = run;
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
        return next;
    }

    protected void findNext() {
        if(hasNext == null) {
            hasNext = false;

            while(count < offset && wrapped.hasNext()) {
                T n = wrapped.next();
                if (countNulls || n != null ) {
                    count++;
                }
            }

            if(count < offsetmax && wrapped.hasNext()) {
                next = wrapped.next();
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
}
