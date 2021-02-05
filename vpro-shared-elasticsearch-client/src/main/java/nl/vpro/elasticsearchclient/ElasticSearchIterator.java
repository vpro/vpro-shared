package nl.vpro.elasticsearchclient;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.stream.Collectors;

import javax.management.ObjectName;

import org.apache.http.HttpEntity;
import org.apache.http.entity.ContentType;
import org.apache.http.nio.entity.NStringEntity;
import org.apache.http.util.EntityUtils;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.elasticsearch.client.*;
import org.meeuw.math.windowed.WindowedEventRate;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import nl.vpro.elasticsearch.ElasticSearchIndex;
import nl.vpro.elasticsearch.ElasticSearchIteratorInterface;
import nl.vpro.jackson2.Jackson2Mapper;
import nl.vpro.jmx.MBeans;
import nl.vpro.util.ThreadPools;
import nl.vpro.util.Version;

import static nl.vpro.elasticsearch.Constants.*;
import static nl.vpro.elasticsearch.Constants.Fields.SOURCE;
import static nl.vpro.elasticsearch.Constants.Methods.METHOD_DELETE;
import static nl.vpro.elasticsearch.Constants.Methods.POST;

/**
 * A wrapper around the Elastic Search scroll interface, to expose it as a simple {@link Iterator}
 * <pre>{@code
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
 * }</pre>
 * @author Michiel Meeuwissen
 * @since 0.47
 */
@Slf4j
public class ElasticSearchIterator<T>  implements ElasticSearchIteratorInterface<T> {

    private static long instances = 0;

    @Getter
    private final long instance = instances++;

    private final Function<JsonNode, T> adapt;
    private final RestClient client;

    protected ObjectNode request;
    private JsonNode response;
    @Getter
    private Long count = -1L;
    private JsonNode hits;
    private String scrollId;
    private Long checkedOrder = 500L;

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

    @Getter
    private Duration duration = Duration.ofMillis(0);


    private final Duration scrollContext;

    @Getter
    @Setter
    private boolean jsonRequests = true;

    @Getter
    private Version<Integer> esVersion = new Version<>(7);

    private Long totalSize = null;
    private TotalRelation totalRelation = TotalRelation.EQUAL_TO;

    private final boolean requestVersion;

    @Getter
    private final  WindowedEventRate rate;

    private final ObjectName objectName;

    public ElasticSearchIterator(RestClient client, Function<JsonNode, T> adapt) {
        this(client, adapt, null, Duration.ofMinutes(1), new Version<>(7), false, true, true, null, null, null);
    }


    @lombok.Builder(builderClassName = "Builder")
    @lombok.SneakyThrows
    protected ElasticSearchIterator(
        @lombok.NonNull RestClient client,
        Function<JsonNode, T> adapt,
        Class<T> adaptTo,
        Duration scrollContext,
        Version<Integer> esVersion,
        boolean _autoEsVersion,
        Boolean jsonRequests,
        Boolean requestVersion,
        String beanName,
        WindowedEventRate rateMeasurerer,
        List<String> routingIds
    ) {
        this.adapt = adapterTo(adapt, adaptTo);
        this.client = client;
        this.scrollContext = scrollContext == null ? Duration.ofMinutes(1) : scrollContext;
        if (_autoEsVersion && esVersion == null) {
            try {
                Response response = client.performRequest(new Request("GET", ""));
                try {
                    JsonNode read = Jackson2Mapper.getLenientInstance()
                        .readerFor(ObjectNode.class)
                        .readValue(response.getEntity().getContent());
                    esVersion = Version.parseIntegers(read.get("version").get("number").asText());
                } finally {
                    EntityUtils.consumeQuietly(response.getEntity());
                }

            } catch (IOException e) {
                throw new IllegalStateException(e);
            }
        }
        if (esVersion != null) {
            if (jsonRequests == null) {
                jsonRequests = esVersion.isNotBefore(5);
            }
            this.esVersion = esVersion;
        }
        this.jsonRequests = jsonRequests == null || jsonRequests;
        this.requestVersion = requestVersion == null || requestVersion;
        if (beanName != null) {
            objectName = MBeans.registerBean(this, instance + "-" + beanName);
        } else {
            objectName = null;
        }

        this.rate = rateMeasurerer == null ? WindowedEventRate.builder()
                .bucketCount(5)
                .bucketDuration(Duration.ofMinutes(1))
                .build() : rateMeasurerer;

        this.routing = routingIds;
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
        if (adapter == null) {
            return jsonNode -> (T) jsonNode;
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
        this.types = types == null ? Collections.emptyList() : types;
        setIndices(indices);
        return request;
    }

    protected void setIndices(Collection<String> indices) {
        this.indices = indices == null ? Collections.emptyList() : indices;
    }

    public ObjectNode getRequest() {
        if (request == null) {
            throw new IllegalStateException("prepareSearch not called");
        }
        return request;
    }

    @Override
    public boolean hasNext() {
        findNext();
        return hasNext;

    }

    protected void findNext() {
        if (needsNext) {
            synchronized (this) {
                long start = System.nanoTime();
                try {
                    if (response == null) {
                        if (! firstBatch()) {
                            return;
                        }
                    }
                    i++;
                    boolean newHasNext = i < hits.get(HITS).size();
                    if (!newHasNext) {
                        nextBatch();
                    } else {
                        hasNext = true;
                    }
                    if (hasNext) {
                        next = adapt.apply(hits.get(HITS).get(i));
                    } else {
                        close();
                    }
                    needsNext = false;
                } finally {
                    duration = duration.plusNanos(System.nanoTime() - start);
                }
            }
        }
    }

    @Override
    public float getFraction() {
        long total = Duration.between(start, Instant.now()).toMillis();
        long es = duration.toMillis();
        return (float) es / total;
    }

    protected boolean firstBatch() {
        // first call only.
        if (request == null) {
            throw new IllegalStateException("prepareSearch not called");
        }
        if (client == null) {
            throw new IllegalStateException("No client");
        }
        try {
            ArrayNode sort = request.withArray(SORT);
            if (sort.isEmpty(null)) {
                log.debug("No explicit sort given, sorting on _doc!");
                QueryBuilder.docOrder(request);
            }

            try(NStringEntity entity = new NStringEntity(request.toString(), ContentType.APPLICATION_JSON)) {
                StringBuilder builder = new StringBuilder();
                if (!indices.isEmpty()) {
                    builder.append("/").append(String.join(",", indices));
                }
                if (types != null && !types.isEmpty()) {
                    if (builder.length() > 0) {
                        builder.append("/");
                    }
                    builder.append(String.join(",", types));
                }
                builder.append(Paths.SEARCH);
                start = Instant.now();
                Request post = new Request(POST, builder.toString());
                post.setEntity(entity);
                if (! scrollContext.isNegative()) {
                    post.addParameter(SCROLL, scrollContext.toMinutes() + "m");
                }
                post.addParameter(VERSION, String.valueOf(this.requestVersion));
                if (routing != null && routing.size() > 0) {
                    post.addParameter(ROUTING, String.join(",", routing));
                }

                HttpEntity responseEntity = null;
                try {
                    Response res = client.performRequest(post);
                    responseEntity = res.getEntity();
                    response = Jackson2Mapper.getLenientInstance().readerFor(JsonNode.class).readTree(responseEntity.getContent());
                } finally {
                    EntityUtils.consumeQuietly(responseEntity);
                }
            }

        } catch (IOException ioe) {
            //log.error(ioe.getMessage());
            throw new RuntimeException("For request " + request.toString() + ":" + ioe.getMessage(), ioe);

        }
        if (hits == null) {
            readResponse();
        }
        String newScrollId = response.get(_SCROLL_ID).asText();
        if (newScrollId != null) {
            log.debug("Scroll id {} -> {}", scrollId, newScrollId);
            scrollId = newScrollId;
            SCROLL_IDS.add(scrollId);
        }

        JsonNode total = hits.get("total");
        if (total instanceof ObjectNode) {
            totalSize = total.get("value").longValue();
        } else {
            totalSize = total.longValue();
        }
        if (totalSize == 0) {
            hasNext = false;
            needsNext = false;
            close();
            return false;
        }
        return true;

    }

    private void nextBatch() {
        if (scrollId != null) {
            try {
                if( count > checkedOrder) {
                    checkedOrder = Long.MAX_VALUE;
                    ArrayNode sort = request.withArray(SORT);
                    if (!DOC.equals(sort.get(0).textValue())) {
                        log.warn("Not sorting on {} (but on {}). This has bad influence on performance", DOC, sort);
                    }
                }

                Request post;
                if (jsonRequests) {
                    ObjectNode scrollRequest = Jackson2Mapper.getInstance().createObjectNode();
                    scrollRequest.put(SCROLL, scrollContext.toMinutes() + "m");
                    scrollRequest.put(SCROLL_ID, scrollId);

                    post = new Request(POST, Paths.SCROLL);
                    post.setJsonEntity(scrollRequest.toString());

                } else {
                    post = new Request(POST, Paths.SCROLL);
                    post.addParameter(SCROLL, scrollContext.toMinutes() + "m");
                    post.setEntity(new NStringEntity(scrollId, ContentType.TEXT_PLAIN));
                }

                HttpEntity responseEntity = null;
                try {
                    Response res = client.performRequest(post);
                    responseEntity = res.getEntity();
                    response = Jackson2Mapper.getLenientInstance()
                            .readerFor(JsonNode.class)
                            .readTree(responseEntity.getContent()
                            );
                } finally {
                    EntityUtils.consumeQuietly(responseEntity);
                }
                log.debug("New scroll");
                if (response.has(_SCROLL_ID)) {
                    String newScrollId = response.get(_SCROLL_ID).asText();
                    if (!scrollId.equals(newScrollId)) {
                        log.info("new scroll id {}", newScrollId);
                        SCROLL_IDS.remove(scrollId);
                        scrollId = newScrollId;
                        SCROLL_IDS.add(scrollId);
                    }
                }
                readResponse();
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
    }

    protected void readResponse() {
        hits = response.get(HITS);
        if (hits != null) {
            JsonNode total  = hits.get("total");
            if (total.has("value")) {
                totalSize = total.get("value").longValue();
            } else {
                totalSize = hits.get("total").longValue();
            }
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
        rate.newEvent();
        return next;
    }



    @Override
    public @NonNull Optional<Long> getSize() {
        findNext();
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
                    case "eq":
                        this.totalRelation = TotalRelation.EQUAL_TO;
                        break;
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




    @Override
    public void close()  {
        if (objectName != null) {
            ThreadPools.backgroundExecutor.schedule(() -> MBeans.unregister(objectName), 2, TimeUnit.MINUTES);
        }
        if (scrollId != null) {
            try {
                Request delete = new Request(METHOD_DELETE, "/_search/scroll/" + scrollId);

                HttpEntity responseEntity = null;
                try {
                    Response res = client.performRequest(delete);
                    responseEntity = res.getEntity();
                    if (res.getStatusLine().getStatusCode() == 200) {
                        log.debug("Deleted {} {}", scrollId, res);
                        SCROLL_IDS.remove(scrollId);
                    } else {
                        log.warn("Something wrong deleting scroll id {} {}", scrollId, res);
                    }
                    scrollId = null;
                } finally {
                    EntityUtils.consumeQuietly(responseEntity);
                }
            } catch (ResponseException re) {
                if (re.getResponse().getStatusLine().getStatusCode() == 404) {
                    log.debug("Not found to delete");
                } else {
                    log.warn(re.getMessage());
                }
                EntityUtils.consumeQuietly(re.getResponse().getEntity());
            } catch (Exception e) {
                log.warn(e.getMessage());
            }
        } else {
            log.debug("no need to close");
        }
    }

    @Override
    public double getSpeed() {
        return rate.getRate();
    }


    public static abstract class AbstractBuilder<T, SELF extends AbstractBuilder<T, SELF>> {

        protected List<String> routingList;
        public AbstractBuilder() {
        }

        @SuppressWarnings("unchecked")
        protected SELF self() {
            return (SELF) this;
        }

        public SELF routing(String routing) {
            if (routingList == null) {
                routingList = new ArrayList<>();
            }
            routingList.add(routing);
            return routingIds(routingList);
        }
        public abstract SELF routingIds(List<String> routingIds);
    }

    public static class Builder<T> extends AbstractBuilder<T, Builder<T>> {



        public Builder<T> autoEsVersion() {
            return _autoEsVersion(true);
        }
        public Builder<T>  elasticsearch(int i) {
            return esVersion(Version.of(i));
        }
    }
}
