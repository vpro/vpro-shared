package nl.vpro.hibernate;

import lombok.extern.slf4j.Slf4j;

import java.util.function.Function;

import org.hibernate.Criteria;
import org.hibernate.Query;
import org.hibernate.ScrollMode;
import org.hibernate.ScrollableResults;

import nl.vpro.util.CloseableIterator;

/**
 * Executes a {@link Criteria} and makes the result accessible as a {@link CloseableIterator}.
 * @author Michiel Meeuwissen
 * @since 1.63
 */
@Slf4j
public class QueryIterator<T> extends ScrollableResultsIterator<T> {


    public QueryIterator(Query criteria, Function<ScrollableResults, T> adapter) {
        super(criteria
            .setReadOnly(true)
            .setCacheable(false)
            .scroll(ScrollMode.FORWARD_ONLY), adapter);
    }

}
