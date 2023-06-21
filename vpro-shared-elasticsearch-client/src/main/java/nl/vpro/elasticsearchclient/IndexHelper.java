package nl.vpro.elasticsearchclient;

import lombok.*;

import java.io.*;
import java.net.ConnectException;
import java.net.URLEncoder;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.*;
import java.util.stream.Collectors;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpEntity;
import org.apache.http.entity.ContentType;
import org.apache.http.nio.entity.NByteArrayEntity;
import org.apache.http.nio.entity.NStringEntity;
import org.apache.http.util.EntityUtils;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.elasticsearch.client.*;
import org.slf4j.*;
import org.slf4j.event.Level;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.base.Suppliers;

import nl.vpro.elasticsearch.*;
import nl.vpro.jackson2.Jackson2Mapper;
import nl.vpro.logging.Slf4jHelper;
import nl.vpro.logging.simple.*;
import nl.vpro.util.*;

import static nl.vpro.elasticsearch.Constants.*;
import static nl.vpro.elasticsearch.Constants.Methods.*;
import static nl.vpro.elasticsearch.ElasticSearchIndex.resourceToObjectNode;
import static nl.vpro.jackson2.Jackson2Mapper.getPublisherInstance;
import static nl.vpro.logging.Slf4jHelper.log;
import static nl.vpro.logging.simple.Slf4jSimpleLogger.slf4j;

/**
 * Some tools to automatically create indices and put mappings and stuff.
 * <p>
 * It is associated with one index and one cluster, and contains the methods to create/delete/update the index settings themselves.
 * <p>
 * Also, it contains utilities to perform some common get/post-operations (like indexing/deleting a node), creating bulk requests, and executing them, where the index name than can be implicit.
 *
 * @author Michiel Meeuwissen
 * @since 0.24
 */
@SuppressWarnings({"UnusedReturnValue", "unused", "resource"})
@Getter
@Setter
public class IndexHelper implements IndexHelperInterface<RestClient>, AutoCloseable {

    private final SimpleLogger log;
    private Supplier<String> indexNameSupplier;
    private Supplier<ObjectNode> settings;
    private List<String> aliases;
    private ESClientFactory clientFactory;
    private final Supplier<ObjectNode> mapping;
    private ObjectMapper objectMapper;
    private File writeJsonDir;
    private ElasticSearchIndex elasticSearchIndex;
    private boolean countAfterCreate = false;
    private final Supplier<Map<String, String>> mdcSupplier;
    private final Distribution distribution;

    CompletableFuture<Info> info = null;


    public static class Builder {

        public Builder log(Logger log){
            this.simpleLogger(slf4j(log));
            return this;
        }

        public Builder log(org.apache.logging.log4j.Logger log){
            this.simpleLogger(Log4j2SimpleLogger.of(log));
            return this;
        }

        public Builder mappingResource(String mapping) {
            return mapping(() -> resourceToObjectNode(mapping));
        }

        public Builder settingsResource(final String resource) {
            return settings(() -> resourceToObjectNode(resource)
            );
        }

        public Builder indexName(final String indexName) {
            return indexNameSupplier(() -> indexName);
        }
    }

    @lombok.Builder(builderClassName = "Builder")
    private IndexHelper(
        SimpleLogger simpleLogger,
        @NonNull ESClientFactory client,
        ElasticSearchIndex elasticSearchIndex,
        Supplier<String> indexNameSupplier,
        Supplier<ObjectNode> settings,
        Supplier<ObjectNode> mapping,
        File writeJsonDir,
        ObjectMapper objectMapper,
        List<String> aliases,
        boolean countAfterCreate,
        Supplier<Map<String, String>> mdcSupplier,
        @Nullable Distribution distribution
        ) {
        if (elasticSearchIndex != null) {
            if (indexNameSupplier == null) {
                indexNameSupplier = elasticSearchIndex::getIndexName;
            }
            if (settings == null) {
                settings = () -> resourceToObjectNode(elasticSearchIndex.getSettingsResource());
            }
            if (mapping == null) {
                mapping = elasticSearchIndex.mapping();
            }
            if (aliases == null || aliases.isEmpty()) {
                // no explicit aliases, use those as specified by index
                aliases = new ArrayList<>(elasticSearchIndex.getAliases());
                // also add the index name itself if that is missing, which is implicit.
                if (! aliases.contains(elasticSearchIndex.getIndexName())) {
                    aliases.add(elasticSearchIndex.getIndexName());
                }
            }
            this.elasticSearchIndex = elasticSearchIndex;
        }
        this.log = simpleLogger == null ? slf4j(LoggerFactory.getLogger(IndexHelper.class)) : simpleLogger;
        this.clientFactory = client;
        this.indexNameSupplier = indexNameSupplier == null ? () -> "" : indexNameSupplier;
        this.settings = settings;
        this.writeJsonDir = writeJsonDir;
        this.objectMapper = objectMapper == null ? getPublisherInstance() : objectMapper;
        this.aliases = aliases == null ? Collections.emptyList() : aliases;
        this.countAfterCreate = countAfterCreate;
        this.mdcSupplier = mdcSupplier == null ? MDC::getCopyOfContextMap : mdcSupplier;
        this.distribution = distribution;
        this.mapping = mapping;
    }

    public static IndexHelper.Builder of(Logger log, ESClientFactory client, ElasticSearchIndex index) {
        return IndexHelper.builder()
            .log(log)
            .client(client)
            .elasticSearchIndex(index)
            ;
    }

    public static IndexHelper.Builder of(org.apache.logging.log4j.Logger log, ESClientFactory client, ElasticSearchIndex index) {
        return IndexHelper.builder()
            .simpleLogger(Log4j2SimpleLogger.of(log))
            .client(client)
            .elasticSearchIndex(index)
            ;
    }

    public static IndexHelper.Builder of(String cat, ESClientFactory client, ElasticSearchIndex index) {
        return IndexHelper.builder()
            .log(LoggerFactory.getLogger(cat))
            .client(client)
            .elasticSearchIndex(index)
            ;
    }


    @Override
    public RestClient client() {
        try {
            return clientAsync((c) -> {
                if (countAfterCreate) {
                    log.info("Index {}: entries: {}", getIndexName(), count(c));
                } else {
                    log.info("Index {}: {}", getIndexName(), c);
                }
            }).get();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException(e);
        } catch (ExecutionException e) {
            if (e.getCause() instanceof ConnectException) {
                clientFactory.invalidate();
            }
            throw new RuntimeException(e);
        }
    }

    /**
     * Returns client after that checks on it are performed.
     */
    public Future<RestClient> clientAsync(Consumer<RestClient> callback) {
        String name = IndexHelper.class.getName();
        if (indexNameSupplier != null) {
            name += "." + indexNameSupplier.get();
        }
        if (clientFactory instanceof AsyncESClientFactory) {
            return ((AsyncESClientFactory) clientFactory).clientAsync(name, callback);
        } else {
            RestClient client = clientFactory.client(name, callback);
            return CompletableFuture.completedFuture(client);
        }
    }


    /**
     * Creates the new associated index
     * @param createIndex options for doing that
     */
    @Override
    @SneakyThrows
    public  void createIndex(CreateIndex createIndex)  {

        if (getIndexName().isEmpty()){
            throw new IllegalStateException("No index name configured");
        }
        final String supplied = indexNameSupplier.get();

        final String indexName;
        if (createIndex.isUseNumberPostfix()) {
            long number = firstNewNumber();
            indexName = supplied + "-" + number;
        } else {
            indexName = supplied;
        }

        ObjectNode request = Jackson2Mapper.getInstance().createObjectNode();

        if (createIndex.isCreateAliases() && (! this.aliases.isEmpty() || createIndex.isUseNumberPostfix())) {
            ObjectNode aliases = request.with("aliases");
            for (String alias : this.aliases) {
                if (alias.equals(indexName)) {
                    continue;
                }
                if (alias.equals(supplied)) {
                    if (aliasExists(alias)) {
                        log.info("Not making alias {} because it exists already");
                        continue;
                    }
                }
                aliases.with(alias);
            }
        }

        ObjectNode  settings = request.set("settings", this.settings.get());
        if (createIndex.isForReindex()){
            forReindex(settings);
        }
        if (createIndex.getShards() != null) {
            ObjectNode index = settings.with("settings").with("index");
            index.put("number_of_shards", createIndex.getShards());
        }
        if (createIndex.getNumberOfReplicas() != null) {
            ObjectNode index = settings.with("settings").with("index");
            index.put("number_of_replicas", createIndex.getNumberOfReplicas());
        }
        if (mapping == null) {
            throw new IllegalStateException("No mappings provided in " + this);
        }
        ObjectNode mappingJson = mapping.get();
        if (elasticSearchIndex != null) {
            BiConsumer<Distribution, ObjectNode> mappingsProcessor = elasticSearchIndex.getMappingsProcessor();
            mappingsProcessor.accept(getInfo().get().getDistribution(), mappingJson);
        }
        request.set("mappings", mappingJson);
        HttpEntity entity = entity(request);

        log.info("Creating index {} with mapping {}: {}", indexName, mapping, request.toString());
        Request req = new Request(PUT, "/" + indexName);
        req.setEntity(entity);
        ObjectNode response = read(client().performRequest(req));


        if (response.get("acknowledged").booleanValue()) {
            String setIndexName = getIndexName();
            if (! Objects.equals(setIndexName, indexName)) {
                setIndexName(indexName);
                log.info("Created index {} (postfixed from {})", getIndexName(), setIndexName);
            } else {
                log.info("Created index {}", getIndexName());
            }
        } else {
            log.warn("Could not create index {}", indexName);
        }

    }

    public final List<String> getIndices() throws IOException {
        Request req = new Request(GET, "/_cat/indices/" + getIndexName() + "-*");

        Response response = client().performRequest(req);
        List<String> result = new ArrayList<>();
        for (String line : IOUtils.toString(response.getEntity().getContent(), Charset.defaultCharset()).split("\\n")) {
            if (StringUtils.isNotBlank(line)) {
                String[] split = line.split("\\s+");
                result.add(split[2]);
            }
        }
        return result;
    }

    public long firstNewNumber() throws IOException {
        long number = 0;
        for (String existing : getIndices()) {
            String[] split = existing.split("-", 2);
            long existingNumber = Long.parseLong(split[1]);
            if (existingNumber >= number) {
                number = existingNumber + 1;
            }
        }
        return number;
    }


    /**
     * For the current index. Reput the settings.
     * <p>
     * Before doing that remove all settings from the settings object, that may not be updated, otherwise ES gives errors.
     *
     * @param postProcessSettings You may want to modify the settings objects even further before putting it to ES.
     */
    @SafeVarargs
    @SneakyThrows
    public final void reputSettings(Consumer<ObjectNode>... postProcessSettings) {
        ObjectNode request = Jackson2Mapper.getInstance().createObjectNode();
        ObjectNode  settings = request.set("settings", this.settings.get());
        ObjectNode index = settings.with("settings").with("index");
        if (!index.has("refresh_interval")) {
            index.put("refresh_interval", "30s");
        }
        // remove stuff that cannot be updated
        index.remove("analysis");
        index.remove("number_of_shards");
        index.remove("sort.order");
        index.remove("sort.field");


        // allow to caller to modify it further
        for (Consumer<ObjectNode> consumer : postProcessSettings) {
            consumer.accept(settings);
        }
        HttpEntity entity = entity(request);
        Request req = createPut(Paths.SETTINGS);
        req.setEntity(entity);
        ObjectNode response = read(client().performRequest(req));

        log.info("settings: {}", response);
    }

    public void reputSettings(boolean forReindex) {
        reputSettings(forReindex ? IndexHelper::forReindex : on -> {});
    }


    @SafeVarargs
    @SneakyThrows
    public final void reputMappings(Consumer<ObjectNode>... consumers) {
        ObjectNode request = mapping.get();
        Distribution distribution = getInfo().get().getDistribution();
        elasticSearchIndex.getMappingsProcessor().accept(distribution, request);

        for(Consumer<ObjectNode> consumer: consumers) {
            consumer.accept(request);
        }
        HttpEntity entity = entity(request);
        Request req = createPut(Paths.MAPPING);
        req.setEntity(entity);
        ObjectNode response = read(client().performRequest(req));

        log.info("mappings: {}", response);
    }

    /**
     * Update  the settings json so that is proper for 'reindexing'
     *
     * @see <a href="https://www.elastic.co/guide/en/elasticsearch/reference/current/reindex-upgrade-remote.html">reindex</a>
     */
    public static void forReindex(ObjectNode  settings) {
        //https://www.elastic.co/guide/en/elasticsearch/reference/current/reindex-upgrade-remote.html
        ObjectNode index = settings.with("settings").with("index");
        index.put("refresh_interval", -1);
        index.put("number_of_replicas", 0);
    }


    /**
     * Checks whether the associated index exists
     */
    @Override
    public boolean checkIndex() {
        try {
            Response response = client().performRequest(createHead(null));
            boolean result =  response.getStatusLine().getStatusCode() != 404;
            EntityUtils.consumeQuietly(response.getEntity());
            return result;
        } catch (IOException e) {
            log.error(e.getMessage(), e);
            return false;
        }
    }


    public void deleteIndex()  {
        try {
            Response delete = client().performRequest(createDelete(null));

            log.info("{}", delete);
            EntityUtils.consumeQuietly(delete.getEntity());
        } catch (IOException e) {
            log.error(e.getMessage(), e);
        }
    }

    public long clearIndex() {
        final List<BulkRequestEntry> bulk = new ArrayList<>();
        try (ElasticSearchIterator<JsonNode> i = ElasticSearchIterator.of(client())) {
            i.prepareSearch(getIndexName());

            while (i.hasNext()) {
                JsonNode node = i.next();
                if (node.has(Fields.ROUTING)) {
                    bulk.add(deleteRequestWithRouting(node.get(Fields.ID).asText(), node.get(Fields.ROUTING).asText()));
                } else {
                    bulk.add(deleteRequest(node.get(Fields.ID).asText()));
                }
            }
        }
        if (bulk.size() > 0) {
            ObjectNode bulkResult = bulk(bulk);
            bulkLogger(LoggerFactory.getLogger(log.getName())).accept(bulkResult);
        }
        return bulk.size();
    }


    /**
     * Issue a 'refresh' command, so we can be sure that index operations are handled.
     * Must used in test cases.
     */
    public boolean refresh() {

        try {
            Response response = client().performRequest(new Request(GET, "/_refresh"));
            JsonNode read = read(response);
            return read != null;
        } catch (IOException e) {
            log.error(e.getMessage(), e);
            return false;
        }
    }

    /**
     * Obtains version of connected elasticsearch deployment
     */
    public String getVersionNumber() {

        try {
            Response response = client().performRequest(new Request(GET, "/"));
            JsonNode read = read(response);
            return read.get("version").get("number").asText();
        } catch (IOException e) {
            log.error(e.getMessage(), e);
            return null;
        }
    }

    /**
     * Obtains version of connected elasticsearch deployment as a {@link nl.vpro.util.IntegerVersion}
     */
    public Version<Integer> getVersion() {
        return Version.parseIntegers(getVersionNumber());
    }

    private Supplier<Map<String, String>> unaliasing;

    public String unalias(String alias) {
        if (unaliasing == null) {
            if (clientFactory == null) {
                log.warn("No client factory, can't implicitly unalias");
                unaliasing = HashMap::new;
            } else {
                unaliasing = Suppliers.memoizeWithExpiration(() -> {
                    Map<String, String> result = new HashMap<>();
                    try {
                        Request request = new Request(GET, "/_cat/aliases");
                        request.setOptions(request.getOptions().toBuilder().addHeader("accept", "application/json"));

                        Response response = client().performRequest(request);
                        ArrayNode read = readArray(response);
                        for (JsonNode i : read) {
                            result.put(i.get("alias").textValue(), i.get(INDEX).textValue());
                        }
                    } catch (IOException e) {
                        log.error(e.getMessage(), e);

                    }
                    return result;
                }, 10, TimeUnit.MINUTES);
            }
        }
        return unaliasing.get().getOrDefault(alias, alias);
    }

    public boolean aliasExists(String alias) throws IOException {
        Request request = new Request(HEAD, "/_alias/" + alias);
        Response response = client().performRequest(request);
        return response.getStatusLine().getStatusCode() == 200;
    }

    public ObjectNode search(ObjectNode request) {
        String indexName = indexNameSupplier == null ? null : indexNameSupplier.get();
        return post((indexName == null ? "" : indexName) + Paths.SEARCH, request);
    }

    public ObjectNode deleteByQuery(ObjectNode request) {
        return post(getIndexName() + Paths.DELETE_BY_QUERY, request);
    }

    public ObjectNode updateByQuery(ObjectNode request) {
        return post(getIndexName() + Paths.UPDATE_BY_QUERY, request);
    }


    @SafeVarargs
    public final Future<ObjectNode> searchAsync(ObjectNode request, Consumer<ObjectNode>... listeners) {
        String indexName = indexNameSupplier == null ? null : indexNameSupplier.get();
        return postAsync((indexName == null ? "" : indexName) + Paths.SEARCH, request, listeners);
    }


    @SafeVarargs
    public final ObjectNode post(String path, ObjectNode request, Consumer<Request>... consumers) {
        return postEntity(path, entity(request), consumers);
    }

    @SafeVarargs
    public final ObjectNode postEntity(String path, HttpEntity entity, Consumer<Request>... consumers) {
        try {

            Request req = new Request(POST, path);
            req.setEntity(entity);
            for (Consumer<Request> c : consumers) {
                c.accept(req);
            }
            log.debug("Posting to {}", path);
            return read(client().performRequest(req));
        } catch (IOException e) {
            log.error(e.getMessage(), e);
            throw new RuntimeException(e);
        }
    }

    public static HttpEntity entity(JsonNode node) {
        return new NStringEntity(saveToString(node), ContentType.APPLICATION_JSON);
    }

    public static HttpEntity entity(byte[] json) {
        return new NByteArrayEntity(json, ContentType.APPLICATION_JSON);
    }

    /**
     * @param path path to post to
     * @param request json to post to that.
     * @param listeners Listeners to process results (e.g. log errors or indexing)
     */
    @SafeVarargs
    public final CompletableFuture<ObjectNode> postAsync(@NonNull String path, @NonNull ObjectNode request, @NonNull Consumer<ObjectNode>... listeners) {
        final CompletableFuture<ObjectNode> future = new CompletableFuture<>();
        clientAsync((client) -> {
            log.debug("posting");
            Request req = new Request(POST, path);
            req.setEntity(entity(request));
            client.performRequestAsync(req, listen(log, request.toString(), future, listeners));
            }
        );
        return future;
    }

    public static Optional<BulkRequestEntry> find(
        @NonNull final Collection<BulkRequestEntry> jobs,
        @NonNull final ObjectNode responseItem) {
        return jobs.stream().filter(
            item -> BulkRequestEntry
                .idFromActionNode(responseItem)
                .equals(item.getId())
        ).findFirst();
    }

    @SafeVarargs
    static protected ResponseListener listen(
        SimpleLogger log,
        @NonNull final String requestDescription,
        @NonNull final CompletableFuture<ObjectNode> future,
        @NonNull Consumer<ObjectNode>... listeners) {
        return new ResponseListener() {
            @Override
            public void onSuccess(Response response) {
                ObjectNode result = read(log, response);
                future.complete(result);
                for (Consumer<ObjectNode> rl : listeners) {
                    rl.accept(result);
                }
            }

            @Override
            public void onFailure(Exception exception) {
                if (exception instanceof ResponseException) {
                    future.completeExceptionally(exception);
                    ResponseException re = (ResponseException) exception;
                    Response response = re.getResponse();
                    try {
                        ObjectNode result = read(log, response);
                        if (result == null) {
                            log.warn("{} No object node found from {}", requestDescription, response);
                            result = Jackson2Mapper.getInstance().createObjectNode();
                        }
                        for (Consumer<ObjectNode> rl : listeners) {
                            rl.accept(result);
                        }
                    } finally {
                        EntityUtils.consumeQuietly(response.getEntity());
                    }
                } else {
                    log.error("{}: {}", requestDescription, exception.getMessage(), exception);
                    future.completeExceptionally(exception);
                    ObjectNode error  = new ObjectNode(Jackson2Mapper.getLenientInstance().getNodeFactory());
                    error.put("errors", true);
                    error.putArray("items");
                    error.put("message", exception.getMessage());
                    for (Consumer<ObjectNode> rl : listeners) {
                        rl.accept(error);
                    }
                }
            }
        };
    }

    public ObjectNode index(String id, Object o) {
        return index(id, o, (node) -> {});
    }

    public ObjectNode index(String id, byte[] o) {
        HttpEntity entity = entity(o);
        return postEntity(indexPath(id), entity(o));
    }

    public ObjectNode index(String id, Object o, Consumer<ObjectNode> sourceConsumer) {
        ObjectNode jsonNode = objectMapper.valueToTree(o);
        sourceConsumer.accept(jsonNode);
        return post(indexPath(id), jsonNode);
    }


    /**
     */
    public ObjectNode indexWithRouting(String id, byte[] o, String routing) {
        return postEntity(indexPath(id), entity(o), req -> req.addParameter(ROUTING, routing));
    }

    /**
     */
    public ObjectNode indexWithRouting(String id, Object o, String routing) {
        return indexWithRouting(id, o, routing, (node) -> {});
    }
    /**
     */
    public ObjectNode indexWithRouting(String id, Object o, String routing, Consumer<ObjectNode> sourceConsumer) {
        ObjectNode jsonNode = objectMapper.valueToTree(o);
        sourceConsumer.accept(jsonNode);
        return post(indexPath(id), jsonNode, req -> req.addParameter(ROUTING, routing));
    }

    public ObjectNode index(BulkRequestEntry indexRequest) {
        return post(
            _indexPath(
                indexRequest.getAction().get(TYPE).textValue(),
                indexRequest.getAction().get(ID).textValue(),
                indexRequest.getAction().get(PARENT).textValue()
            ),
            indexRequest.getSource()
        );
    }


    protected String indexPath(String id) {
        return _indexPath(DOC, id, null);
    }

    protected String _indexPath(String type, @Nullable String id, @Nullable String parent) {
        String path = getIndexName() + "/" + type + (id == null ? "" : ("/" + encode(id)));
        if (parent != null) {
            path += "?parent=" + parent;
        }
        return path;
    }


    public ObjectNode delete(String id) {
        try {
            client().performRequest(createDelete("/" + DOC + "/" + encode(id)));
        } catch (IOException e) {
            log.error(e.getMessage(), e);
        }
        return null;
    }


    @SafeVarargs
    public final Future<ObjectNode> deleteAsync(
        @NonNull BulkRequestEntry deleteRequest,
        @NonNull Consumer<ObjectNode>... listeners) {
        return deleteAsync(deleteRequest.getAction().get(TYPE).textValue(), deleteRequest.getSource().get(ID).textValue(), listeners);
    }


    @SafeVarargs
    private CompletableFuture<ObjectNode> deleteAsync(String type, @NonNull String id, @NonNull Consumer<ObjectNode>... listeners) {
        final CompletableFuture<ObjectNode> future = new CompletableFuture<>();

        client().performRequestAsync(createDelete( "/" + type + "/" + encode(id)),
            listen(log, "delete " + type + "/" + id, future, listeners)
        );
        return future;
    }

    public  Optional<JsonNode> get(String id){
        return _get(id, null);
    }

    public  List<@NonNull Optional<JsonNode>> mget(String... ids){
        return mget(Arrays.asList(ids));
    }

    public  List<@NonNull Optional<JsonNode>> mgetWithRouting(RoutedId... ids){
        return mgetWithRouting(Arrays.asList(ids));
    }

    public  List<@NonNull Optional<JsonNode>> mget(Collection<String> ids) {
        return mgetWithRouting(ids.stream().map(i -> new RoutedId(i, null)).collect(Collectors.toList()));
    }

    public  List<@NonNull Optional<JsonNode>> mgetWithRouting(Collection<RoutedId> ids){
        if (ids.size() == 0) {
            return Collections.emptyList();
        }
        Request get = createGet("/_mget");
        //get.addParameter("routing", "AUTO_WEKKERWAKKER");

        ObjectNode body = Jackson2Mapper.getInstance().createObjectNode();
        ArrayNode array = body.withArray("docs");
        for (RoutedId id :ids ) {
            ObjectNode doc = array.addObject();
            doc.put(Fields.ID, id.id);
            if (id.routing != null) {
                //doc.put(Fields.ROUTING, id.routing); // something wrong with doc?
                doc.put("routing", id.routing);
            }
        }

        get.setJsonEntity(saveToString(body));
        List<Optional<JsonNode>> result = new ArrayList<>();
        try {
            Response response = client().performRequest(get);
            ObjectNode objectNode = read(response);
            ArrayNode docs = objectNode.withArray("docs");

            for (JsonNode n : docs) {
                boolean found = n.get("found").asBoolean();
                if (found) {
                    result.add(Optional.of(n));
                } else {
                    result.add(Optional.empty());
                }
            }

            return result;
        } catch (IOException e) {
            log.error(e.getMessage(), e);
        }
        return result;
    }

    public  Optional<JsonNode> getWithRouting(String id, String routing){
        return _get(id, routing);
    }

    public  Optional<JsonNode> getWithRouting(RoutedId id){
        return _get(id.id, id.routing);
    }


    protected Optional<JsonNode> _get(String id, String routing) {
        try {
            Request get = createGet("/" + DOC + "/" + encode(id));
            if (routing != null){
                get.addParameter("routing", routing);
            }
            Response response = client()
                .performRequest(get);

            return Optional.of(read(response));
        } catch (ResponseException re) {
            if (re.getResponse().getStatusLine().getStatusCode() == 404) {
                return Optional.empty();
            }
            if (re.getResponse().getStatusLine().getStatusCode() >= 400) {
                log.error(re.getMessage(), re);
            } else {
                log.debug(re.getMessage());
            }
            return Optional.empty();
        } catch (IOException e) {
            log.error(e.getMessage(), e);
            throw new RuntimeException(e);
        }
    }




    private <T> Optional<T> _get(Collection<String> type, String id, Function<JsonNode, T> adapter) {
        ObjectNode body = Jackson2Mapper.getInstance().createObjectNode();
        ArrayNode array = body.withArray("docs");
        for (String t : type) {
            ObjectNode doc = array.addObject();
            doc.put(Fields.ID, id);
            doc.put(Fields.TYPE, t);
        }
        ObjectNode post = post(getIndexName() + "/_mget", body);

        ArrayNode result = post.withArray("docs");
        for (JsonNode n : result) {
            if (n.get("found").booleanValue()) {
                return Optional.of(adapter.apply(n));
            }
        }
        return Optional.empty();
    }

    public <T> Optional<T> get(String id, Function<JsonNode, T> adapter) {
        return get(id).map(adapter);
    }


    public Optional<JsonNode> getSource(String id) {
        return get(id).map(jn -> jn.get(Fields.SOURCE));
    }


    /**
     * Reads a response to json, logging to {@link #log}
     */
    public  ObjectNode read(Response response) {
        return read(log, response);
    }

    /**
     * Reads a response to json, logging to {@link #log}
     */
    public  ArrayNode readArray(Response response) {
        return read(log, response, ArrayNode.class);
    }

    /**
     * Reads a response to json, using {@link Jackson2Mapper#getLenientInstance()}, catch exceptions,
     * make sure resources are closed.
     */
    public static <T extends JsonNode> T read(SimpleLogger log, Response response, Class<T> clazz) {
        try {
            HttpEntity entity = response.getEntity();
            try (InputStream inputStream = entity.getContent()) {
                return Jackson2Mapper.getLenientInstance()
                    .readerFor(clazz)
                    .readValue(inputStream);
            } finally {
                EntityUtils.consumeQuietly(entity);
            }

        } catch (IOException e) {
            log.error(e.getMessage(), e);
            return null;
        }
    }

    public static ObjectNode read(SimpleLogger log, Response response) {
        if (response.getHeader("content-type").startsWith(ContentType.APPLICATION_JSON.getMimeType())) {
            return read(log, response, ObjectNode.class);
        } else {
            ObjectNode n = Jackson2Mapper.getInstance().createObjectNode();
            try {
                n.put("error", true);
                n.put("body", IOUtils.toString(response.getEntity().getContent(), StandardCharsets.UTF_8));
                n.put("statusCode", response.getStatusLine().getStatusCode());
                n.put("statusLine", response.getStatusLine().toString());
                n.put("requestLine", response.getRequestLine().toString());
            } catch (IOException e) {
                log.error(e.getMessage(), e);
            }
            return n;
        }
    }

    String encode(@NonNull String id) {
        return URLEncoder.encode(id, StandardCharsets.UTF_8);
    }

    /**
     * Creates a {@link BulkRequestEntry} for indexing an object with given id.
     * @param id The id of the object to use
     * @param o The object to index
     * @param consumers consumer a list of {@link ObjectNode} {@link Consumer}s that will be called on the source node after that is constructed from the object to index
     */
    @SafeVarargs
    public final BulkRequestEntry indexRequest(String id, Object o, Consumer<ObjectNode>... consumers) {
        ObjectNode actionLine = objectMapper.createObjectNode();
        ObjectNode index = actionLine.with(INDEX);
        index.put(Fields.ID, id);
        index.put(Fields.INDEX, getIndexName());

        ObjectNode objectNode  = objectMapper.valueToTree(o);
        return BulkRequestEntry.builder()
            .action(actionLine)
            .source(objectNode)
            .unalias(this::unalias)
            .mdc(mdcSupplier.get())
            .sourceConsumer((on) -> {
                for (Consumer<ObjectNode> c : consumers) {
                    c.accept(on);
                }
            }).build();

    }
    /**
     * Creates a {@link BulkRequestEntry} for indexing an object with given id.
     * @param id The id of the object to use
     * @param o The object to index
     * @param consumers a list of {@link ObjectNode} {@link Consumer}s that will be called on the the source node after that is constructed from the object to index
     */
    @SafeVarargs
    public final BulkRequestEntry updateRequest(String id, Object o, Consumer<ObjectNode>... consumers) {
        ObjectNode actionLine = objectMapper.createObjectNode();
        ObjectNode update = actionLine.with(UPDATE);
        update.put(Fields.ID, id);
        update.put(Fields.INDEX, getIndexName());
        update.put(RETRY_ON_CONFLICT, 3);
        ObjectNode updateNode  = objectMapper.createObjectNode();
        ObjectNode objectNode = objectMapper.valueToTree(o);

        updateNode.set(Fields.DOC, objectNode);
        updateNode.put(Fields.DOC_AS_UPSERT, false);
        return BulkRequestEntry.builder()
            .action(actionLine)
            .source(updateNode)
            .unalias(this::unalias)
            .mdc(mdcSupplier.get())
            .sourceConsumer((on) -> {
                for (Consumer<ObjectNode> c : consumers) {
                    ObjectNode doc = (ObjectNode) on.get(Fields.DOC);
                    c.accept(doc);
                }}
            ).build();

    }

    /**
     * Creates a {@link BulkRequestEntry} for indexing an object with given id, and routing
     * @param id The id of the object to use
     * @param o The object to index
     * @param routing The routing to use
     * @param consumers consumer a list of {@link ObjectNode} {@link Consumer}s that will be called on the the source node after that is constructed from the object to index
     */
    @SafeVarargs
    public final BulkRequestEntry indexRequestWithRouting(String id, Object o, String routing, Consumer<ObjectNode>... consumers) {
        BulkRequestEntry request =
            indexRequest(id, o, consumers);
        request.getAction()
            .with(INDEX)
            .put(ROUTING, routing);
        return request;
    }


    public BulkRequestEntry deleteRequest(String id) {
        ObjectNode actionLine = Jackson2Mapper.getInstance().createObjectNode();
        ObjectNode index = actionLine.with(DELETE);
        index.put(Fields.ID, id);
        index.put(Fields.INDEX, getIndexName());
        return new BulkRequestEntry(actionLine, null, this::unalias, mdcSupplier.get());
    }

    public BulkRequestEntry deleteRequestWithRouting(String id, String routing) {
        BulkRequestEntry request  = deleteRequest(id);
        request.getAction().with(DELETE)
            .put(ROUTING, routing);
        return request;
    }

    public ObjectNode bulk(Collection<BulkRequestEntry> request) {
        if (request.isEmpty()) {
            return objectMapper.createObjectNode();
        } else {
            try {
                Request req = new Request(POST, Paths.BULK);
                req.setEntity(bulkEntity(request));

                writeJson(log, writeJsonDir, request);
                return  read(
                    client().performRequest(req)
                );
            } catch (IOException e) {
                log.error(e.getMessage(), e);
                return null;
            }
        }
    }


    @SafeVarargs
    public final CompletableFuture<ObjectNode> bulkAsync(
        Collection<BulkRequestEntry> request, Consumer<ObjectNode>... listeners) {
        return bulkAsync(log, writeJsonDir, client(), request, listeners);
    }


    /**
     *
     * @param request Collection of {@link BulkRequestEntry}s
     * @param listeners Listeners to process results (e.g. log errors or indexing)
     */
    @SafeVarargs
    public static CompletableFuture<ObjectNode> bulkAsync(
        SimpleLogger log,
        File jsonDir,
        RestClient client,
        Collection<BulkRequestEntry> request,
        Consumer<ObjectNode>... listeners) {
        final CompletableFuture<ObjectNode> future = new CompletableFuture<>();
        if (request.size() == 0) {
            return CompletableFuture.completedFuture(null);
        }

        Request req = new Request(POST, Paths.BULK);
        req.setEntity(bulkEntity(request));
        writeJson(log, jsonDir, request);

        client.performRequestAsync(req,
            listen(log, "" + request.size() + " bulk operations", future, listeners)
        );
        return future;
    }

    protected static HttpEntity bulkEntity(Collection<BulkRequestEntry> request) {
        StringBuilder builder = new StringBuilder();
        for (BulkRequestEntry n : request) {
            builder.append(n.getAction());
            builder.append("\n");
            n.use();
            if (n.getSource() != null) {
                builder.append(saveToString(n.getSource()));
                builder.append("\n");
            }
        }
        return new NStringEntity(builder.toString(), ContentType.APPLICATION_JSON);
    }

    static protected String saveToString(JsonNode jsonNode) {
        String value = jsonNode.toString();
        return value.replaceAll("\\p{Cc}", "");

    }

    @Override
    @SneakyThrows
    public long count() {
        return count(client());
    }

    @SneakyThrows
    protected long count(RestClient client) {
        Request get = createGet(Paths.COUNT);
        return parseCount(client.performRequest(get));
    }


    public void countAsync(final Consumer<Long> consumer) {
        Request get = createGet(Paths.COUNT);
        clientAsync(c -> c.performRequestAsync(get, new ResponseListener() {
            @Override
            public void onSuccess(Response response) {
                long count = parseCount(response);
                consumer.accept(count);
            }

            @Override
            public void onFailure(Exception e) {
                log.warn("For count on {}", this, e);
            }
        }));
    }

    protected long parseCount(Response response) {
        JsonNode result = read(response);
        return result.get("count").longValue();
    }

    public  Optional<JsonNode> getActualSettings() throws IOException {
        Request get = createGet(Paths.SETTINGS);
        Response response = client()
            .performRequest(get);
        if (response.getStatusLine().getStatusCode() == 200) {
            JsonNode result = read(response);
            return Optional.of(result.fields().next().getValue().get("settings").get(INDEX));
        } else {
            log.warn("For {} -> {}", get, response);
            return Optional.empty();
        }
    }

    @SneakyThrows
    public Duration getRefreshInterval()  {
        JsonNode refreshInterval = getActualSettings().map(jn -> jn.get("refresh_interval")).orElse(null);
        if (refreshInterval == null) {
            return Duration.ofSeconds(30);
        } else {
            return TimeUtils.parseDuration(refreshInterval.textValue()).orElseThrow(IllegalArgumentException::new);
        }
    }

    public void setIndexName(String indexName) {
        this.indexNameSupplier = () -> indexName;
    }


    public String getIndexName() {
        return indexNameSupplier.get();
    }

    public String getClusterName() {
        try {
            return getClusterNameAsync().get();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException(e);
        } catch (ExecutionException e) {
            throw new RuntimeException(e);
        }
    }


    public final CompletableFuture<String> getClusterNameAsync() {
        return getInfo().thenApply(Info::getClusterName);
    }

    @SafeVarargs
    public static CompletableFuture<String> getClusterName(SimpleLogger log, RestClient client, final Consumer<String>... callBacks) {
        return getInfo(log, client).thenApply(Info::getClusterName);
    }

    public CompletableFuture<Info> getInfo() {
        if (info == null) {
            info = getInfo(log, client()).thenApply(
                i -> {
                    if (IndexHelper.this.distribution != null && i.getDistribution() != IndexHelper.this.distribution) {
                        log.warn("Determined distribution {} is not equal equal to explicitly configured one {} (Taking the explicit one)", i.getDistribution(), this.distribution);
                        return i.withDistribution(this.distribution);
                    }
                    return i;
                }
            );

        }
        return info;
    }

    public static synchronized CompletableFuture<Info> getInfo(SimpleLogger log, RestClient client) {
        final CompletableFuture<Info> future = new CompletableFuture<>();
        client.performRequestAsync(new Request(GET, "/"), new ResponseListener() {
            @Override
            public void onSuccess(Response response) {
                ByteArrayOutputStream out = new ByteArrayOutputStream();
                try (InputStream inputStream = response.getEntity().getContent()) {
                    ObjectNode jsonNode = (ObjectNode) Jackson2Mapper.getLenientInstance().readTree(inputStream);
                    String clusterName = jsonNode.get("cluster_name").textValue();
                    String name = jsonNode.get("name").textValue();
                    JsonNode version = jsonNode.with("version");
                    String buildFlavor = version.has("build_flavor") ? version.get("build_flavor").textValue() : "default";
                    Distribution distribution = version.has("distribution") ? Distribution.valueOf(version.get("distribution").textValue().toUpperCase()): "default".equals(buildFlavor) ? Distribution.ELASTICSEARCH : Distribution.OPENSEARCH;
                    Info info = Info.builder()
                        .clusterName(clusterName)
                        .name(name)
                        .distribution(distribution)
                        .buildFlavor(buildFlavor)
                        .version(version.has("number") ? IntegerVersion.parseIntegers(version.get("number").textValue()) :  null)
                        .luceneVersion(version.has("lucene_version") ? IntegerVersion.parseIntegers(version.get("lucene_version").textValue()) :  null)
                        .build();
                    log.info("Connected with {} -> {}", jsonNode, info);
                    future.complete(info);
                } catch (IOException e) {
                    log.error(e.getMessage(), e);
                    future.completeExceptionally(e);
                }
            }

            @Override
            public void onFailure(Exception exception) {
                if (exception instanceof ConnectException) {
                    log.info(exception.getMessage());
                } else {
                    log.error("Error getting info from {}: {}", client, exception.getMessage(), exception);
                }
                future.completeExceptionally(exception);
            }

        });
        return future;
    }

    public Consumer<ObjectNode> indexLogger() {
        return indexLogger(log, () -> "");
    }

    public Consumer<ObjectNode> indexLogger(Logger logger) {
        return indexLogger(logger, () -> "");
    }

    public Consumer<ObjectNode> indexLogger(Logger logger, Supplier<String> prefix) {
        return indexLogger(Slf4jSimpleLogger.of(logger), prefix);
    }

    public Consumer<ObjectNode> indexLogger(SimpleLogger logger, Supplier<String> prefix) {
        return jsonNode -> {
            String index = jsonNode.get(Fields.INDEX).textValue();
            String type = jsonNode.get(Fields.TYPE).textValue();
            String id = jsonNode.get(Fields.ID).textValue();
            Integer version = jsonNode.hasNonNull(Fields.VERSION) ? jsonNode.get(Fields.VERSION).intValue() : null;
            if (jsonNode.hasNonNull(Fields.ERROR)) {
                logger.error("{}{}/{}/{}/{}: {}", prefix.get(), clientFactory.logString(), index, type, encode(id), jsonNode);
            } else {
                if (logger.isInfoEnabled()) {
                    logger.info("{}{}/{}/{}/{} version: {}", prefix.get(), clientFactory.logString(), index, type, encode(id), version);
                }
                logger.debug("{}{}", prefix.get(), jsonNode);
            }
        };
    }

    public Consumer<ObjectNode> deleteLogger(@NonNull Logger logger) {
        return deleteLogger(logger, () -> "");
    }

    public Consumer<ObjectNode> deleteLogger(@NonNull Logger logger, @NonNull Supplier<String> prefix) {
        return deleteLogger(slf4j(logger), prefix);
    }

    public Consumer<ObjectNode> deleteLogger(@NonNull SimpleLogger logger, @NonNull Supplier<String> prefix) {
        return jsonNode -> {
            boolean found = jsonNode.has("found") && jsonNode.get("found").booleanValue();
            String index = jsonNode.get(Fields.INDEX).textValue();
            String type = jsonNode.get(Fields.TYPE).textValue();
            String id = jsonNode.get(Fields.ID).textValue();
            if (logger.isInfoEnabled()) {
                if (found) {
                    int version = jsonNode.has(Fields.VERSION) ? jsonNode.get(Fields.VERSION).intValue() : -1;

                    logger.info("{}{}/{}/{}/{} version: {}", prefix.get(), clientFactory.logString(), index, type, encode(id), version);

                } else {
                    logger.info("{}{}/{}/{}/{} (not found)", prefix.get(), clientFactory.logString(), index, type, encode(id));
                }
            }
            if (logger.isDebugEnabled()) {
                logger.debug("{}{} {}", prefix.get(), clientFactory.logString(), jsonNode);
            }
        };
    }


    public Consumer<ObjectNode> bulkLogger() {
        return bulkLogger(log, log, log);
    }

    public Consumer<ObjectNode> bulkLogger(
        @NonNull Logger indexLog,
        @NonNull Logger deleteLog,
        @NonNull Logger updateLog) {
        return bulkLogger(slf4j(indexLog), slf4j(deleteLog), slf4j(updateLog));
    }


    public Consumer<ObjectNode> bulkLogger(
        @NonNull SimpleLogger indexLog,
        @NonNull SimpleLogger deleteLog,
        @NonNull SimpleLogger updateLog) {
        final StringBuilder logPrefix = new StringBuilder();
        final Consumer<ObjectNode> indexLogger = indexLogger(indexLog, logPrefix::toString);
        final Consumer<ObjectNode> deleteLogger = deleteLogger(deleteLog, logPrefix::toString);
        final Consumer<ObjectNode> updateLogger = indexLogger(updateLog, logPrefix::toString);

        return consumeBulkResult((i, total) -> {
                logPrefix.setLength(0);
                logPrefix.append(i).append('/').append(total).append(' ');
            },
            deleteLogger,
            indexLogger,
            updateLogger,
            n -> log.warn("{}Unrecognized bulk response {}", logPrefix, n)
        );
    }

    public static Consumer<ObjectNode> consumeBulkResult(
            ObjIntConsumer<Integer> each,
            Consumer<ObjectNode> deletes,
            Consumer<ObjectNode> indexes,
            Consumer<ObjectNode> updates,
            Consumer<JsonNode> unrecognized) {
        return jsonNode -> {
            final ArrayNode items = jsonNode.withArray("items");
            int i = 0;
            int total = items.size();
            for (JsonNode n : items) {
                each.accept(++i, total);
                ObjectNode on = (ObjectNode) n;
                boolean recognized = false;
                if (on.has(DELETE)) {
                    deletes.accept(on.with(DELETE));
                    recognized = true;
                }
                if (n.has(INDEX)) {
                    indexes.accept(on.with(INDEX));
                    recognized = true;
                }
                if (n.has(UPDATE)) {
                    updates.accept(on.with(UPDATE));
                    recognized = true;
                }
                if (! recognized) {
                    unrecognized.accept(n);
                }
            }
        };

    }

    public Consumer<ObjectNode> bulkLogger(@NonNull Logger logger) {
        return bulkLogger(logger, () -> Level.INFO, () ->  Level.INFO);
    }

    public Consumer<ObjectNode> bulkLogger(@NonNull Logger logger, @NonNull Supplier<Level> singleLevel, @NonNull Supplier<Level> combinedLevel) {
        return bulkLogger(logger, singleLevel, combinedLevel, true);
    }

    public Consumer<ObjectNode> bulkLogger(
        @NonNull Logger logger,
        @NonNull Supplier<Level> singleLevel,
        @NonNull Supplier<Level> combinedLevel,
        boolean singleVerbose) {
        return jsonNode -> {
            final ArrayNode items = jsonNode.withArray("items");
            String index = null;
            final List<String> deleted = new ArrayList<>();
            final List<String> indexed = new ArrayList<>();
            for (JsonNode n : items) {
                ObjectNode on = (ObjectNode) n;
                if (singleVerbose) {
                    log(logger, singleLevel.get(), "{}", on);
                }
                if (on.has(DELETE)) {
                    final ObjectNode delete = on.with(DELETE);
                    index = delete.get(Fields.INDEX).textValue();
                    String type = delete.get(Fields.TYPE).textValue();
                    String id = delete.get(Fields.ID).textValue();
                    String logEntry = handleResponse(delete, type, id);
                    deleted.add(logEntry);
                    if (! singleVerbose) {
                        log(logger, singleLevel.get(), "delete:{}", logEntry);
                    }
                    continue;
                }
                if (n.has(INDEX)) {
                    ObjectNode indexResponse = on.with(INDEX);
                    index = indexResponse.get(Fields.INDEX).textValue();
                    String type = indexResponse.get(Fields.TYPE).textValue();
                    String id = indexResponse.get(Fields.ID).textValue();
                    String logEntry = handleResponse(indexResponse, type, id);
                    indexed.add(logEntry);
                    if (! singleVerbose) {
                        log(logger, singleLevel.get(), "indexed:{}", logEntry);
                    }
                    continue;
                }
                if (n.has(UPDATE)) {
                    ObjectNode indexResponse = on.with(UPDATE);
                    index = indexResponse.get(Fields.INDEX).textValue();
                    String type = indexResponse.get(Fields.TYPE).textValue();
                    String id = indexResponse.get(Fields.ID).textValue();
                    String logEntry = handleResponse(indexResponse, type, id);
                    indexed.add(logEntry);
                    if (! singleVerbose) {
                        log(logger, singleLevel.get(), "updated:{}", logEntry);
                    }
                    continue;
                }
                logger.warn("Unrecognized bulk response {}", n);

            }

            if (Slf4jHelper.isEnabled(logger, combinedLevel.get())) {
                if (!indexed.isEmpty()) {
                    if (!deleted.isEmpty()) {
                        log(logger, combinedLevel.get(), "{} {} indexed: {}, revoked: {}", clientFactory.logString(), index, indexed, deleted);
                    } else {
                        log(logger, combinedLevel.get(), "{} {} indexed: {}", clientFactory.logString(), index, indexed);
                    }
                } else if (!deleted.isEmpty()) {
                    log(logger, combinedLevel.get(), "{} {} revoked: {}", clientFactory.logString(), index, deleted);
                } else {
                    log(logger, combinedLevel.get(), "{} {} bulk request didn't yield result", clientFactory.logString(), index);
                }
            }
        };
    }

    static  String handleResponse(@NonNull JsonNode indexResponse, @NonNull String type, @NonNull String id) {
        int status = -1;
        if (indexResponse.has("status")) {
            status = indexResponse.get("status").intValue();
        }
        StringBuilder logEntry = new StringBuilder();
        if (status != 200) {
            logEntry.append("status:").append(status);
        }
        if (indexResponse.has("result")) {
            String result = indexResponse.get("result").textValue();
            logEntry.append(" ").append(logEntry(type, id, result));
        }
        if (indexResponse.has("error")) {
            logEntry.append(" ").append(indexResponse.get("error").toString());
        }
        return logEntry.toString();
    }

    static private String logEntry(@NonNull String type, @NonNull String id, @NonNull String result){
        if (DOC.equals(type)) {
            return id + ":" + result;
        } else {
            return type + ":" + id + ":" + result;
        }
    }

    public void setWriteJsonDir(@Nullable File file) {
        if (file != null) {
            if (! file.exists()) {
                if (file.mkdirs()) {
                    log.info("Created {}", file);
                } else {
                    log.info("Could not create {}", file);
                }
            }
        }
        this.writeJsonDir = file;
    }

    protected Request createRequest(@NonNull String method, @Nullable String operation) {
        if (operation == null) {
            operation = "";
        }
        if (StringUtils.isNotEmpty(operation)) {
            assert operation.startsWith("/");
        }
        return new Request(method, "/" + getIndexName() + operation);
    }

    protected Request createGet(@Nullable String operation) {
        return createRequest(GET, operation);
    }

    protected Request createHead(@Nullable String operation) {
        return createRequest(HEAD, operation);
    }

    protected Request createPut(@Nullable String operation) {
        return createRequest(PUT, operation);
    }

    protected Request createDelete(@Nullable String operation) {
        return createRequest(Methods.METHOD_DELETE, operation);
    }

    @Override
    public void close() {
        log.info("Closing {}", clientFactory);
        CloseableIterator.closeQuietly(clientFactory);
        clientFactory = null;
    }

    @Override
    public String toString() {
        StringBuilder builder =  new StringBuilder("IndexHelper{" + getIndexName() + " ");
        builder.append(clientFactory);
        if (aliases != null && ! aliases.isEmpty()) {
            builder.append(", ").append(aliases);
        }
        if (writeJsonDir != null) {
            builder.append(", writeJsonDir=").append(writeJsonDir);
        }
        builder.append('}');
        return builder.toString();
    }

    static protected void writeJson(SimpleLogger log, File writeJsonDir, Collection<BulkRequestEntry> requests) {
        for (BulkRequestEntry request: requests) {
            ObjectNode actionLine = request.getAction();
            if (actionLine.has("index")) {
                writeJson(log, writeJsonDir, actionLine.get("index").get(Fields.ID).textValue(), request.getSource());
            }
        }
    }

    static protected void writeJson(SimpleLogger log, File  writeJsonDir, String id, JsonNode jsonNode) {
        if (writeJsonDir != null) {
            File file = new File(writeJsonDir, id.replace(
                File.separator, "_"
            ) + ".json");
            try (OutputStream out = Files.newOutputStream(file.toPath())) {
                Jackson2Mapper.getPrettyInstance().writeValue(out, jsonNode);
                log.info("Wrote {}", file);
            } catch (IOException e) {
                log.error(e.getMessage(), e);
            }
        }
    }

    public void waitForHealth() throws InterruptedException {
        waitForHealth(client(), log);
    }

    public static void waitForHealth(RestClient client, SimpleLogger log) throws InterruptedException {
        while (true) {
            try {
                Request request = new Request(GET, "/_cat/health");
                request.setOptions(request.getOptions()
                    .toBuilder()
                    .addHeader("accept", "application/json"));

                Response response = client.performRequest(request);
                ArrayNode health = Jackson2Mapper.getLenientInstance()
                    .readerFor(ArrayNode.class)
                    .readValue(response.getEntity().getContent());

                String status = health.get(0).get("status").textValue();
                log.info("status {}", status);
                boolean serviceIsUp = "green".equals(status) || "yellow".equals(status);
                if (serviceIsUp) {
                    break;
                }
            } catch (RuntimeException ee) {
                throw ee;
            } catch (Exception e){
                log.info(e.getMessage());
            }
            TimeUnit.SECONDS.sleep(2);
        }
    }

    @Getter
    public static class RoutedId {
        final String id;
        final String routing;

        public RoutedId(String id, String routing) {
            this.id = id;
            this.routing = routing;
        }
    }
}
