package nl.vpro.elasticsearchclient;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.function.Function;
import java.util.stream.*;

import org.apache.http.HttpEntity;
import org.apache.http.entity.ContentType;
import org.apache.http.nio.entity.NStringEntity;
import org.elasticsearch.client.*;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import nl.vpro.elasticsearch.ElasticSearchIndex;
import nl.vpro.elasticsearch.ElasticSearchIteratorInterface;
import nl.vpro.jackson2.Jackson2Mapper;
import nl.vpro.util.Version;

import static nl.vpro.elasticsearch.Constants.*;
import static nl.vpro.elasticsearch.Constants.Fields.SOURCE;

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
public class ElasticSearchIterator<T>  implements ElasticSearchIteratorInterface<T> {

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
    @Deprecated
    private Collection<String> types;

    @Getter
    @Setter
    private Collection<String> routing;

    @Getter
    private Instant start;

    private Duration scrollContext;

    @Getter
    @Setter
    private boolean jsonRequests = true;

    private boolean search_type_scan = false;

    private Version<Integer> esVersion = new Version<>(7);

    private Long totalSize = null;
    private TotalRelation totalRelation = TotalRelation.EQUAL_TO;


    public ElasticSearchIterator(RestClient client, Function<JsonNode, T> adapt) {
        this(client, adapt, null, Duration.ofMinutes(1), new Version<>(7), false, true);
    }


    @lombok.Builder(builderClassName = "Builder")
    private ElasticSearchIterator(
        RestClient client,
        Function<JsonNode, T> adapt,
        Class<T> adaptTo,
        Duration scrollContext,
        Version<Integer> esVersion,
        boolean _autoEsVersion,
        Boolean jsonRequests

    ) {
        this.adapt = adapterTo(adapt, adaptTo);
        this.client = client;
        this.scrollContext = scrollContext == null ? Duration.ofMinutes(1) : scrollContext;
        if (_autoEsVersion && esVersion == null) {
            try {
                Response response = client.performRequest(new Request("GET", ""));
                JsonNode read = Jackson2Mapper.getLenientInstance()
                    .readerFor(ObjectNode.class)
                    .readValue(response.getEntity().getContent());
                esVersion = Version.parseIntegers(read.get("version").get("number").asText());

            } catch (IOException e) {
                throw new IllegalStateException(e);
            }
        }
        if (esVersion != null) {
            if (jsonRequests == null) {
                jsonRequests = esVersion.isNotBefore(5);
            }
            search_type_scan = esVersion.isBefore(2);
        }
        this.jsonRequests = jsonRequests == null || jsonRequests;
    }


    public static <T> Function<JsonNode, T> adapterTo(Class<T> clazz) {
        return jsonNode -> {
            try {
                return Jackson2Mapper.getLenientInstance()
                    .treeToValue(jsonNode.get(SOURCE), clazz);
            } catch (Exception e) {
                return null;

            }
        };
    }

    private static <T> Function<JsonNode, T> adapterTo(Function<JsonNode, T> adapter, Class<T> clazz) {
        if (adapter != null && clazz != null) {
            throw new IllegalArgumentException();
        }
        if (clazz != null) {
            return jsonNode -> {
                try {
                    return Jackson2Mapper.getLenientInstance()
                        .treeToValue(jsonNode.get(SOURCE), clazz);
                } catch (Exception e) {
                    return null;

                }
            };
        }
        return adapter;
    }



    public static ElasticSearchIterator<JsonNode> of(RestClient client) {
        return ElasticSearchIterator.builderOf(client).build();
    }


    public static ElasticSearchIterator.Builder<JsonNode> builderOf(RestClient client) {
        return ElasticSearchIterator.<JsonNode>builder()
            .client(client)
            .adapt(jn -> jn);
    }

    public static ElasticSearchIterator<JsonNode> sources(RestClient client) {
        return ElasticSearchIterator.sourcesBuilder(client)
            .build();
    }

     public static ElasticSearchIterator.Builder<JsonNode> sourcesBuilder(RestClient client) {
        return ElasticSearchIterator.<JsonNode>builder()
            .client(client)
            .adapt(jn -> jn.get(Fields.SOURCE));
    }

    @Deprecated
    public ObjectNode prepareSearch(Collection<String> indices, Collection<String> types) {
        return _prepareSearch(indices, types);
    }


    @Deprecated
    public ObjectNode prepareSearch(String indices, String... types) {
        return _prepareSearch(Collections.singletonList(indices), Arrays.asList(types));
    }

    public ObjectNode prepareSearch(String index) {
        return _prepareSearch(Collections.singletonList(index), null);
    }


    public ObjectNode prepareSearchOnIndices(String... indices) {
        return _prepareSearch(Arrays.asList(indices), null);
    }

    public ObjectNode prepareSearch(ElasticSearchIndex... indices) {
        return _prepareSearch(Arrays.stream(indices).map(ElasticSearchIndex::getIndexName).collect(Collectors.toList()), null);
    }


    protected  ObjectNode _prepareSearch(Collection<String> indices, Collection<String> types) {
        request = Jackson2Mapper.getInstance().createObjectNode();
        this.indices = indices == null ? Collections.emptyList() : indices;
        this.types = types == null ? Collections.emptyList() : types;
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
                    ArrayNode sort = request.withArray("sort");
                    if (sort.isEmpty(null)) {
                        log.debug("No explicit sort given, sorting on _doc!");
                        QueryBuilder.docOrder(request);
                    }

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
                    if (search_type_scan) {
                        post.addParameter("search_type", "scan");
                    }
                    if (routing != null) {
                        post.addParameter("routing", String.join(",", routing));
                    }
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
                JsonNode total = hits.get("total");
                if (total instanceof ObjectNode) {
                    totalSize = total.get("value").longValue();
                } else {
                    totalSize = total.longValue();
                }
                if (totalSize == 0) {
                    hasNext = false;
                    needsNext = false;
                    return;
                }

            }

            i++;
            boolean newHasNext = i < hits.get(HITS).size();
            if (!newHasNext) {
                if (scrollId != null) {
                    try {
                        Request post;
                        if (jsonRequests) {
                            ObjectNode scrollRequest = Jackson2Mapper.getInstance().createObjectNode();
                            scrollRequest.put(SCROLL, scrollContext.toMinutes() + "m");
                            scrollRequest.put(SCROLL_ID, scrollId);

                            post = new Request("POST", "/_search/scroll");
                            post.setJsonEntity(scrollRequest.toString());

                        } else {
                            post = new Request("POST", "/_search/scroll");
                            post.addParameter(SCROLL, scrollContext.toMinutes() + "m");
                            post.setEntity(new NStringEntity(scrollId, ContentType.TEXT_PLAIN));
                        }
                        Response res = client.performRequest(post);
                        response = Jackson2Mapper.getLenientInstance()
                            .readerFor(JsonNode.class)
                            .readTree(res.getEntity().getContent()
                            );
                        log.debug("New scroll");
                        if (response.has(_SCROLL_ID)) {
                            String newScrollId = response.get(_SCROLL_ID).asText();
                            if (!scrollId.equals(newScrollId)) {
                                log.info("new scroll id {}", newScrollId);
                                scrollId = newScrollId;
                            }
                        }

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
        if (hits != null) {
            JsonNode total  = hits.get("total");
            if (total.has("value")) {
                totalSize = total.get("value").longValue();
            } else {
                totalSize = hits.get("total").longValue();
            }
        }
        return Optional.ofNullable(this.totalSize);
    }


    @Override
    public Optional<TotalRelation> getSizeQualifier() {
        findNext();
        if (hits != null) {
            JsonNode total  = hits.get("total");
            if (total.has("relation")) {
                String relation = total.get("relation").asText();
                switch (relation) {
                    case "eq": this.totalRelation = TotalRelation.EQUAL_TO;
                    default:
                        log.info("Unrecognized {}", relation);
                }
            }
        }
        return Optional.ofNullable(this.totalRelation);
    }


    @Override
    public String toString() {
        return client + " " + request + " " + count;
    }

    public Optional<Instant> getETA() {
        if (getCount() != null && getCount() != 0 && getTotalSize().isPresent()) {
            Duration duration = Duration.between(start, Instant.now());
            Duration estimatedTotalDuration = Duration.ofNanos((long) (duration.toNanos() * (getTotalSize().get().doubleValue() / getCount())));
            return Optional.of(start.plus(estimatedTotalDuration));
        } else {
            return Optional.empty();
        }
    }


    @Override
    public void close()  {

        if (scrollId != null) {
            try {
                Request delete = new Request("DELETE", "/_search/scroll/" + scrollId);
                Response res = client.performRequest(delete);
                log.debug("Deleted {}", res);
                scrollId = null;
            } catch (ResponseException re) {
                if (re.getResponse().getStatusLine().getStatusCode() == 404) {
                    log.debug("Not found to delete");
                } else {
                    log.warn(re.getMessage());
                }
            } catch (Exception e) {
                log.warn(e.getMessage());
            }
        } else {
            log.debug("no need to close");
        }
    }


    public static class Builder<T> {

        public Builder<T> autoEsVersion() {
            return _autoEsVersion(true);
        }
        public Builder<T> elasticsearch(int i) {
            return esVersion(Version.of(i));
        }
    }
}
