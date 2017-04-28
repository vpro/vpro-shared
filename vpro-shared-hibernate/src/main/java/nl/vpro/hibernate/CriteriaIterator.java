package nl.vpro.hibernate;

import lombok.extern.slf4j.Slf4j;

import java.util.NoSuchElementException;
import java.util.function.Function;

import org.hibernate.Criteria;
import org.hibernate.ScrollMode;
import org.hibernate.ScrollableResults;

import nl.vpro.util.CloseableIterator;

/**
 * Executes a {@link Criteria} and makes the result accessible as a {@link CloseableIterator}.
 * @author Michiel Meeuwissen
 * @since 1.63
 */
@Slf4j
public class CriteriaIterator<T> implements CloseableIterator<T> {

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
}
