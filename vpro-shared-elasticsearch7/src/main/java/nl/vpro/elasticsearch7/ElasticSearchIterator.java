package nl.vpro.elasticsearch7;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.function.Function;

import org.elasticsearch.action.ActionFuture;
import org.elasticsearch.action.search.*;
import org.elasticsearch.client.Client;
import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.search.SearchHit;

import nl.vpro.elasticsearch.ElasticSearchIteratorInterface;

/**
 * A wrapper around the Elastic Search scroll interface.
 *
 * @author Michiel Meeuwissen
 * @since 0.47
 */
@Slf4j
public class ElasticSearchIterator<T>  implements ElasticSearchIteratorInterface<T> {

    final Function<SearchHit, T> adapt;
    final Client client;

    @Getter
    @Setter
    boolean version = true;

    SearchRequestBuilder builder;
    SearchResponse response;
    SearchHit[] hits;
    private long count = -1;

    boolean hasNext;
    int i = -1;
    T next;
    boolean needsNext;


    public static ElasticSearchIterator<SearchHit> searchHits(Client client) {
        return new ElasticSearchIterator<>(client, sh -> sh);
    }

    public ElasticSearchIterator(Client client, Function<SearchHit, T> adapt) {
        this(client, adapt, false);
    }

    @lombok.Builder
    protected ElasticSearchIterator(Client client, Function<SearchHit, T> adapt, boolean version) {
        this.adapt = adapt;
        this.client = client;
        needsNext = true;
        this.version = version;
    }

    public SearchRequestBuilder prepareSearch(String... indices) {
        builder = client.prepareSearch(indices);
        builder.setVersion(true);
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
        if (needsNext) {
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
                    close();
                    return;
                }
            }

            i++;
            boolean newHasNext = i < hits.length;
            if (!newHasNext) {
                if (response.getScrollId() != null) {
                    response = client
                        .prepareSearchScroll(response.getScrollId())
                        .setScroll(new TimeValue(60000))
                        .execute()
                        .actionGet();
                    hits = response.getHits().getHits();
                    i = 0;
                    hasNext = hits.length > 0;
                } else {
                    log.warn("No scroll id found, so not possible to scroll next batch");
                    hasNext = false;
                }
            } else {
                hasNext = true;
            }
            if (hasNext) {
                next = adapt.apply(hits[i]);
            } else {
                close();
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
        return response == null ? Optional.empty() : Optional.of(response.getHits().getTotalHits().value);
    }

    @Override
    public Optional<TotalRelation> getSizeQualifier() {
        findNext();
        return response == null ? Optional.empty() : Optional.of(TotalRelation.valueOf(response.getHits().getTotalHits().relation.name()));
    }

    @Override
    public Long getCount() {
        return count;
    }


    @Override
    public void close()  {
        if (response != null && response.getScrollId() != null) {
            ClearScrollRequest clearScrollRequest = new ClearScrollRequest();
            clearScrollRequest.addScrollId(response.getScrollId());
            ActionFuture<ClearScrollResponse> clearScrollResponseActionFuture = client.clearScroll(clearScrollRequest);
            log.debug("{}", clearScrollResponseActionFuture);
            response = null;
        } else {
            log.debug("no need to close");
        }
    }

}
