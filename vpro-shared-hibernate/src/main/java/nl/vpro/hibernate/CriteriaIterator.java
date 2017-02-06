package nl.vpro.hibernate;

import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.function.Function;

import org.hibernate.Criteria;
import org.hibernate.ScrollMode;
import org.hibernate.ScrollableResults;

/**
 * @author Michiel Meeuwissen
 * @since 1.63
 */
public class CriteriaIterator<T> implements Iterator<T> {

    private final ScrollableResults scrollableResults;
    private final Function<ScrollableResults, T> adapter;

    private Boolean hasNext = null;
    private T next;

    public CriteriaIterator(Criteria criteria, Function<ScrollableResults, T> adapter) {
        scrollableResults =
        criteria
            .setReadOnly(true)
            .setCacheable(false)
            .scroll(ScrollMode.FORWARD_ONLY);
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
                next = adapter.apply(scrollableResults);
            }
        }
    }


}
