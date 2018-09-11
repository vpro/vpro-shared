package nl.vpro.elasticsearchclient;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.function.Function;

import org.apache.http.HttpEntity;
import org.apache.http.entity.ContentType;
import org.apache.http.nio.entity.NStringEntity;
import org.elasticsearch.client.Response;
import org.elasticsearch.client.ResponseException;
import org.elasticsearch.client.RestClient;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import nl.vpro.jackson2.Jackson2Mapper;
import nl.vpro.util.CountedIterator;

import static nl.vpro.elasticsearchclient.Constants.*;

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
    @Getter
    private Long count = -1L;
    private JsonNode hits;
    private String scrollId;

    boolean hasNext;
    int i = -1;
    T next;
    boolean needsNext;
    Collection<String> indices;
    Collection<String> types;

    @Getter
    Instant start;

    public ElasticSearchIterator(RestClient client, Function<JsonNode, T> adapt) {
        this.adapt = adapt;
        this.client = client;
        needsNext = true;
    }

    public static ElasticSearchIterator<JsonNode> of(RestClient client) {
        return new ElasticSearchIterator<>(client, jn -> jn);
    }

    public ObjectNode prepareSearch(Collection<String> indices, Collection<String> types) {
        request = Jackson2Mapper.getInstance().createObjectNode();
        this.indices = indices == null ? Collections.emptyList() : indices;
        this.types = types == null ? Collections.emptyList() : types;
        return request;
    }


    public ObjectNode prepareSearch(String indices, String... types) {
        return prepareSearch(Collections.singletonList(indices), Arrays.asList(types));
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
                    params.put(SCROLL, "1m");
                    StringBuilder builder = new StringBuilder();
                    if (! indices.isEmpty()) {
                        builder.append(String.join(",", indices));
                    }
                    if (!types.isEmpty()) {
                        if (builder.length() > 0) {
                            builder.append("/");
                        }
                        builder.append(String.join(",", types));
                    }
                    builder.append("/_search");
                    start = Instant.now();
                    Response res = client.performRequest("POST", builder.toString(), params, entity);
                    response = Jackson2Mapper.getLenientInstance().readerFor(JsonNode.class).readTree(res.getEntity().getContent());
                } catch (IOException ioe) {
                    //log.error(ioe.getMessage());
                    throw new RuntimeException("For request " + request.toString() + ":" + ioe.getMessage(), ioe);

                }
                if (hits == null) {
                    hits = response.get(HITS);
                }
                scrollId = response.get(_SCROLL_ID).asText();
                if (hits.get(HITS).size() == 0) {
                    hasNext = false;
                    needsNext = false;
                    return;
                }
            }

            i++;
            boolean newHasNext = i < hits.get(HITS).size();
            if (!newHasNext) {
                if (scrollId != null) {
                    ObjectNode scrollRequest = Jackson2Mapper.getInstance().createObjectNode();
                    scrollRequest.put(SCROLL, "1m");
                    scrollRequest.put(SCROLL_ID, scrollId);
                    try {
                        Response res = client.performRequest("POST", "/_search/scroll", Collections.emptyMap(), new NStringEntity(scrollRequest.toString(), ContentType.APPLICATION_JSON));
                        response = Jackson2Mapper.getLenientInstance().readerFor(JsonNode.class).readTree(res.getEntity().getContent());
                        log.debug("New scroll");
                        hits = response.get(HITS);
                        i = 0;
                        hasNext = hits.get(HITS).size() > 0;
                    } catch(ResponseException re) {
                        log.warn(re.getMessage());
                        hits = null;
                        hasNext = false;
                    } catch (IOException ioe) {
                        log.error(ioe.getMessage());
                        throw new RuntimeException("For request " + request.toString() + ":" + ioe.getMessage(), ioe);
                    }
                } else {
                    log.warn("No scroll id found, so not possible to scroll next batch");
                    hasNext = false;
                }
            } else {
                hasNext = true;
            }
            if (hasNext) {
                next = adapt.apply(hits.get(HITS).get(i));
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
    public String toString() {
        return client + " " + request + " " + count;
    }

    public Optional<Instant> getETA() {
        if (getCount() != null && getCount() != 0 && getTotalSize().isPresent()) {
            Duration duration = Duration.between(start, Instant.now());
            Duration estimatedTotalDuration = Duration.ofMillis(duration.toMillis() * getTotalSize().get() / getCount());
            return Optional.of(start.plus(estimatedTotalDuration));
        } else {
            return Optional.empty();
        }
    }

}
