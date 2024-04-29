package nl.vpro.hibernate;

import lombok.extern.slf4j.Slf4j;
import org.hibernate.ScrollMode;
import org.hibernate.ScrollableResults;
import org.hibernate.query.Query;

import java.util.function.Function;


import nl.vpro.util.CloseableIterator;

/**
 * Makes a Scroll and makes the result accessible as a {@link CloseableIterator}.
 * @author Michiel Meeuwissen
 * @since 1.63
 */
@Slf4j
public class QueryIterator<T> extends ScrollableResultsIterator<T> {


    public QueryIterator(Query<T> criteria, Function<ScrollableResults<T>, T> adapter) {
        super(criteria
            .setReadOnly(true)
            .setCacheable(false)
            .scroll(ScrollMode.FORWARD_ONLY), adapter);
    }

}
