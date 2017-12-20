package nl.vpro.elasticsearch;

import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.util.*;
import java.util.function.Function;

import org.apache.http.HttpEntity;
import org.apache.http.entity.ContentType;
import org.apache.http.nio.entity.NStringEntity;
import org.elasticsearch.client.Response;
import org.elasticsearch.client.RestClient;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import nl.vpro.jackson2.Jackson2Mapper;
import nl.vpro.util.CountedIterator;

/**
 * A wrapper around the Elastic Search scroll interface.

 * @author Michiel Meeuwissen
 * @since 0.47
 */
@Slf4j
public class ElasticSearchIterator<T>  implements CountedIterator<T> {


    final Function<JsonNode, T> adapt;
    final RestClient client;

    ObjectNode request;
    JsonNode response;
    private long count = -1;
    private JsonNode hits;
    private String scrollId;

    boolean hasNext;
    int i = -1;
    T next;
    boolean needsNext = true;

    String[] indices;




    public ElasticSearchIterator(RestClient client, Function<JsonNode, T> adapt) {
        this.adapt = adapt;
        this.client = client;
        needsNext = true;
    }

    public  static ElasticSearchIterator<JsonNode> of(RestClient client) {
        return new ElasticSearchIterator<>(client, jn -> jn);
    }

    public ObjectNode prepareSearch(String... indices) {
        request = Jackson2Mapper.getInstance().createObjectNode();
        request.with("query");
        this.indices= indices;
        return request;
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
                if (request == null) {
                    throw new IllegalStateException("prepareSearch not called");
                }
                try {
                    HttpEntity entity = new NStringEntity(request.toString(), ContentType.APPLICATION_JSON);
                    Map<String, String> params = new HashMap<>();
                    params.put("scroll", "1m");
                    Response res = client.performRequest("POST", indices[0] + "/_search", params, entity);

                    response = Jackson2Mapper.getLenientInstance().readerFor(JsonNode.class).readTree(res.getEntity().getContent());
                } catch (IOException ioe) {
                    log.error(ioe.getMessage());

                }
                if (hits == null) {
                    hits = response.get("hits");
                }
                scrollId = response.get("_scroll_id").asText();
                if (hits.get("hits").size() == 0) {
                    hasNext = false;
                    needsNext = false;
                    return;
                }
            }

            i++;
            boolean newHasNext = i < hits.get("hits").size();
            if (!newHasNext) {
                if (scrollId != null) {
                    ObjectNode scrollRequest = Jackson2Mapper.getInstance().createObjectNode();
                    scrollRequest.put("scroll", "1m");
                    scrollRequest.put("scroll_id", scrollId);
                    try {
                        Response res = client.performRequest("POST", "/_search/scroll", Collections.emptyMap(), new NStringEntity(scrollRequest.toString(), ContentType.APPLICATION_JSON));
                        response = Jackson2Mapper.getLenientInstance().readerFor(JsonNode.class).readTree(res.getEntity().getContent());
                        log.debug("New scroll");
                    } catch (IOException ioe) {
                        log.error(ioe.getMessage());
                    }

                    hits = response.get("hits");
                    i = 0;
                    hasNext = hits.get("hits").size() > 0;
                } else {
                    log.warn("No scroll id found, so not possible to scroll next batch");
                    hasNext = false;
                }
            } else {
                hasNext = true;
            }
            if (hasNext) {
                next = adapt.apply(hits.get("hits").get(i));
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
        return Optional.of(hits.get("total").longValue());
    }

    @Override
    public Long getCount() {
        return count;
    }

}
