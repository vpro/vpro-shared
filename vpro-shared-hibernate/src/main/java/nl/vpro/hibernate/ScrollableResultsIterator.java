package nl.vpro.hibernate;

import lombok.extern.slf4j.Slf4j;
import org.hibernate.ScrollableResults;

import java.util.NoSuchElementException;
import java.util.function.Function;


import nl.vpro.util.CloseableIterator;

/**

 * @author Michiel Meeuwissen
 * @since 1.63
 */
@Slf4j
public class ScrollableResultsIterator<T> implements CloseableIterator<T> {

    private final ScrollableResults<T> scrollableResults;
    private final Function<ScrollableResults<T>, T> adapter;

    private Boolean hasNext = null;
    private T next;

    public ScrollableResultsIterator(ScrollableResults<T> r, Function<ScrollableResults<T>, T> adapter) {
        scrollableResults = r;
        this.adapter = adapter;
    }

    @Override
    public boolean hasNext() {
        findNext();
        return hasNext;

    }

    @Override
    public T next() {
        findNext();
        if (hasNext) {
            hasNext = null;
            return next;
        } else {
            throw new NoSuchElementException();
        }
    }

    protected void findNext() {
        if (hasNext == null) {
            hasNext = scrollableResults.next();
            if (hasNext) {
                while(true) {
                    try {
                        next = adapter.apply(scrollableResults);
                        break;
                    } catch (Exception e) {
                        log.warn(e.getMessage(), e);
                    }
                }
            } else {
                try {
                    close();
                } catch (Exception e) {
                    log.error(e.getMessage(), e);
                }
            }
        }
    }


    @Override
    public void close()  {
        this.scrollableResults.close();
    }

    @Override
    public String toString() {
        return "Iterator[" + this.scrollableResults + "]";

    }
}
