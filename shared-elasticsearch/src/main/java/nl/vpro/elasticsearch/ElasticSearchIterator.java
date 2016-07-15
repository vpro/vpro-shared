package nl.vpro.elasticsearch;

import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.function.Function;

import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.Client;
import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.search.SearchHit;

/**
 * TODO untested.
 * @author Michiel Meeuwissen
 * @since 0.47
 */
public class ElasticSearchIterator<T>  implements Iterator<T> {

    final Function<SearchHit, T> adapt;
    final SearchResponse response;
    final Client client;

    SearchHit[] hits;
    boolean hasNext;
    int i = -1;
    T next;
    boolean needsNext = true;

    public ElasticSearchIterator(SearchResponse response, Client client, Function<SearchHit, T> adapt) {
        this.response = response;
        this.hits = response.getHits().getHits();
        this.adapt = adapt;
        this.client = client;
        if (this.hits.length == 0) {
            needsNext = false;
            hasNext = false;

        }
    }
    @Override
    public boolean hasNext() {
        findNext();
        return hasNext;

    }

    protected void findNext() {
        if (needsNext) {
            i++;
            boolean newHasNext = i < hits.length;
            if (!newHasNext) {
                hits = client.prepareSearchScroll(response.getScrollId()).setScroll(new TimeValue(60000)).execute().actionGet().getHits().getHits();
                if (hits.length == 0) {
                    hasNext = false;
                } else {
                    hasNext = true;
                }
            } else {
                hasNext = true;
            }
            next = adapt.apply(hits[i]);
        }

    }
    @Override
    public T next() {
        findNext();
        if (!hasNext) {
            throw new NoSuchElementException();
        }
        needsNext = true;
        return next;
    }

}
