package nl.vpro.elasticsearch;

import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.function.Function;

import org.elasticsearch.action.search.SearchRequestBuilder;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.Client;
import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.search.SearchHit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import nl.vpro.util.CountedIterator;

/**
 * A wrapper around the Elastic Search scroll interface.
 *
 * See
 * @author Michiel Meeuwissen
 * @since 0.47
 */
public class ElasticSearchIterator<T>  implements CountedIterator<T> {

    private static final Logger LOG = LoggerFactory.getLogger(ElasticSearchIterator.class);


    final Function<SearchHit, T> adapt;
    final Client client;

    SearchRequestBuilder builder;
    SearchResponse response;
    SearchHit[] hits;
    private long count = -1;

    boolean hasNext;
    int i = -1;
    T next;
    boolean needsNext = true;


    public ElasticSearchIterator(Client client, Function<SearchHit, T> adapt) {
        this.adapt = adapt;
        this.client = client;
        needsNext = true;
    }

    public SearchRequestBuilder prepareSearch(String... indices) {
        builder = client.prepareSearch(indices);
        builder.setScroll(TimeValue.timeValueSeconds(60));
        builder.setSize(1000);
        return builder;
    }

    @Override
    public boolean hasNext() {
        findNext();
        return hasNext;

    }

    protected void findNext() {
        if (response == null) {
            // first call only.
            if (builder == null) {
                throw new IllegalStateException("prepareSearch not called");
            }
            response = builder.get();
            hits = response.getHits().getHits();
            if (hits.length == 0) {
                hasNext = false;
                needsNext = false;
            }
        }
        if (needsNext) {
            i++;
            boolean newHasNext = i < hits.length;
            if (!newHasNext) {
                if (response.getScrollId() != null) {
                    response = client.prepareSearchScroll(response.getScrollId()).setScroll(new TimeValue(60000)).execute().actionGet();
                    hits = response.getHits().getHits();
                    i = 0;
                    if (hits.length == 0) {
                        hasNext = false;
                    } else {
                        hasNext = true;
                    }
                } else {
                    LOG.warn("No scroll id found, so not possible to scroll next batch");
                    hasNext = false;
                }
            } else {
                hasNext = true;
            }
            if (hasNext) {
                next = adapt.apply(hits[i]);
            }
            needsNext = false;
        }

    }
    @Override
    public T next() {
        findNext();
        if (!hasNext) {
            throw new NoSuchElementException();
        }
        count++;
        needsNext = true;
        return next;
    }

    @Override
    public Optional<Long> getSize() {
        findNext();
        return Optional.of(response.getHits().getTotalHits());
    }

    @Override
    public Long getCount() {
        return count;
    }

}
