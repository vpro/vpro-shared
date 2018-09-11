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
import org.elasticsearch.client.Request;
import org.elasticsearch.client.Response;
import org.elasticsearch.client.ResponseException;
import org.elasticsearch.client.RestClient;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import nl.vpro.jackson2.Jackson2Mapper;
import nl.vpro.util.CountedIterator;

import static nl.vpro.elasticsearchclient.Constants.*;

/**
 * A wrapper around the Elastic Search scroll interface, to expose it as a simple {@link Iterator}
 * {@code
 *        ElasticSearchIterator<JsonNode> i = ElasticSearchIterator.sources(client);
 *        JsonNode search = i.prepareSearch("pageupdates-publish");
 *        // fill your request here
 *        i.forEachRemaining((node) -> {
 *             String url = node.get("url").textValue();
 *             if (i.getCount() % 1000 == 0) {
 *                 log.info("{}: {}", i.getCount(), url);
 *
 *             }
 *         });
 *
 * }*
 * @author Michiel Meeuwissen
 * @since 0.47
 */
@Slf4j
public class ElasticSearchIterator<T>  implements CountedIterator<T> {

    private final Function<JsonNode, T> adapt;
    private final RestClient client;

    private ObjectNode request;
    private JsonNode response;
    @Getter
    private Long count = -1L;
    private JsonNode hits;
    private String scrollId;

    private boolean hasNext;
    private int i = -1;
    private T next;
    private boolean needsNext = true;
    private Collection<String> indices;
    private Collection<String> types;

    @Getter
    private Instant start;

    private Duration scrollContext;

    public ElasticSearchIterator(RestClient client, Function<JsonNode, T> adapt) {
        this(client, adapt, Duration.ofMinutes(1));
    }


    @lombok.Builder(builderClassName = "Builder")
    private ElasticSearchIterator(
        RestClient client,
        Function<JsonNode, T> adapt,
        Duration scrollContext

    ) {
        this.adapt = adapt;
        this.client = client;
        this.scrollContext = scrollContext == null ? Duration.ofMinutes(1) : scrollContext;
    }


    public static ElasticSearchIterator<JsonNode> of(RestClient client) {
        return ElasticSearchIterator.<JsonNode>builder()
            .client(client)
            .adapt(jn -> jn)
            .build();
    }

    public static ElasticSearchIterator<JsonNode> sources(RestClient client) {
        return ElasticSearchIterator.<JsonNode>builder()
            .client(client)
            .adapt(jn -> jn.get(SOURCE))
            .build();
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
                    Request post = new Request("POST", builder.toString());
                    post.setEntity(entity);
                    post.addParameter(SCROLL, scrollContext.toMinutes() + "m");
                    Response res = client.performRequest(post);
                    response = Jackson2Mapper.getLenientInstance().readerFor(JsonNode.class).readTree(res.getEntity().getContent());
                } catch (IOException ioe) {
                    //log.error(ioe.getMessage());
                    throw new RuntimeException("For request " + request.toString() + ":" + ioe.getMessage(), ioe);

                }
                if (hits == null) {
                    hits = response.get(HITS);
                }
                scrollId = response.get(_SCROLL_ID).asText();
                log.debug("Scroll id {}", scrollId);
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
                    scrollRequest.put(SCROLL, scrollContext.toMinutes() + "m");
                    scrollRequest.put(SCROLL_ID, scrollId);
                    try {
                        Request post = new Request("POST", "/_search/scroll");
                        post.setEntity(new NStringEntity(scrollRequest.toString(), ContentType.APPLICATION_JSON));
                        Response res = client.performRequest(post);
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
