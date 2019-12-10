package nl.vpro.elasticsearchclient;

import lombok.*;

import java.io.*;
import java.net.URLEncoder;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.*;
import java.util.stream.Collectors;

import org.apache.commons.io.IOUtils;
import org.apache.http.HttpEntity;
import org.apache.http.entity.ContentType;
import org.apache.http.nio.entity.NStringEntity;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.elasticsearch.client.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import nl.vpro.elasticsearch.*;
import nl.vpro.jackson2.Jackson2Mapper;
import nl.vpro.util.Pair;
import nl.vpro.util.Version;

import static nl.vpro.elasticsearch.Constants.*;
import static nl.vpro.elasticsearch.ElasticSearchIndex.resourceToString;
import static nl.vpro.jackson2.Jackson2Mapper.getPublisherInstance;

/**
 * Some tools to automaticly create indices and put mappings and stuff.
 * @author Michiel Meeuwissen
 * @since 0.24
 */
@Getter
@Setter
public class IndexHelper implements IndexHelperInterface<RestClient> {

    private final Logger log;
    private Supplier<String> indexNameSupplier;
    private Supplier<String> settings;
    private List<String> aliases;
    private ESClientFactory clientFactory;
    private final Map<String, Supplier<String>> mappings = new HashMap<>();
    private ObjectMapper objectMapper;
    private File writeJsonDir;
    private ElasticSearchIndex elasticSearchIndex;


    public static class Builder {
        private final Map<String, Supplier<String>> mappings = new HashMap<>();

        @Deprecated
        public Builder mapping(String type, Supplier<String> mapping) {
            this.mappings.put(type, mapping);
            return this;
        }


        @Deprecated
        public Builder mappingResource(String type, String mapping) {
            return mapping(type, () -> resourceToString(mapping));
        }

        public Builder mappingResource(String mapping) {
            String[] split = mapping.split("/");
            String fileName = split[split.length - 1];
            int dot = fileName.lastIndexOf(".");
            String type = fileName.substring(0, dot);
            return mapping(type, () -> resourceToString(mapping));
        }
        public Builder mappings(Map<String, Supplier<String>> mappings) {
            this.mappings.putAll(mappings);
            return this;
        }

        public Builder settingsResource(final String resource) {
            return settings(() -> resourceToString(resource)
            );
        }

        public Builder indexName(final String indexName) {
            return indexNameSupplier(() -> indexName);
        }
    }


    @lombok.Builder(builderClassName = "Builder")
    private IndexHelper(
        Logger log,
        @lombok.NonNull  ESClientFactory client,
        ElasticSearchIndex elasticSearchIndex,
        Supplier<String> indexNameSupplier,
        Supplier<String> settings,
        Map<String, Supplier<String>> mappings,
        File writeJsonDir,
        ObjectMapper objectMapper,
        List<String> aliases
        ) {
        if (elasticSearchIndex != null) {
            if (indexNameSupplier == null) {
                indexNameSupplier = elasticSearchIndex::getIndexName;
            }
            if (settings == null) {
                settings = () -> resourceToString(elasticSearchIndex.getSettingsResource());
            }
            this.mappings.putAll(elasticSearchIndex.mappingsAsMap());
            if (aliases == null || aliases.isEmpty()) {
                aliases = new ArrayList<>(elasticSearchIndex.getAliases());
                if (! aliases.contains(elasticSearchIndex.getIndexName())) {
                    aliases.add(elasticSearchIndex.getIndexName());
                }
            }
            this.elasticSearchIndex = elasticSearchIndex;
        }
        this.log = log == null ? LoggerFactory.getLogger(IndexHelper.class) : log;
        this.clientFactory = client;
        this.indexNameSupplier = indexNameSupplier == null ? () -> "" : indexNameSupplier;
        this.settings = settings;
        if (mappings != null) {
            this.mappings.putAll(mappings);
        }
        this.writeJsonDir = writeJsonDir;
        this.objectMapper = objectMapper == null ? getPublisherInstance() : objectMapper;
        this.aliases = aliases == null ? Collections.emptyList() : aliases;


    }


    public static IndexHelper of(Logger log, ESClientFactory client, String indexName, String objectType) {
        return IndexHelper.builder().log(log)
            .client(client)
            .indexName(indexName)
            .settingsResource("es/setting.json")
            .mappingResource(objectType, String.format("es/%s.json", objectType))
            .build();
    }

    public static IndexHelper of(Logger log, ESClientFactory client, Supplier<String> indexName, String objectType) {
        return IndexHelper.builder().log(log)
            .client(client)
            .indexNameSupplier(indexName)
            .settingsResource("es/setting.json")
            .mappingResource(objectType, String.format("es/%s.json", objectType))
            .build();
    }

    public static IndexHelper.Builder of(Logger log, ESClientFactory client, ElasticSearchIndex index) {
        return IndexHelper.builder()
            .log(log)
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
    public IndexHelper mapping(String type, Supplier<String> mapping) {
        mappings.put(type, mapping);
        return this;
    }

    @Override
    public RestClient client() {
        try {
            return clientAsync((c) -> {}).get();
        } catch (InterruptedException | ExecutionException e) {
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
            RestClient client = clientFactory.client(name);
            callback.accept(client);
            return CompletableFuture.completedFuture(client);
        }

    }

    @Override
    @SneakyThrows
    public  void createIndex(CreateIndex createIndex)  {

        if (getIndexName().isEmpty()){
            throw new IllegalStateException("No index name configured");
        }
        String supplied = indexNameSupplier.get();

        String indexName = createIndex.isUseNumberPostfix() ? supplied + "-0" :  supplied;

        ObjectNode request = Jackson2Mapper.getInstance().createObjectNode();

        if (createIndex.isCreateAliases() && (! this.aliases.isEmpty() || createIndex.isUseNumberPostfix())) {
            ObjectNode aliases = request.with("aliases");
            for (String alias : this.aliases) {
                aliases.with(alias);
            }
        }

        ObjectNode  settings = request.set("settings", Jackson2Mapper.getInstance().readTree(this.settings.get()));
        if (createIndex.isForReindex()){
            forReindex(settings);
        }
        if (createIndex.getShards() != null) {
            ObjectNode index = settings.with("settings").with("index");
            index.put("number_of_shards", createIndex.getShards());
        }
        if (mappings.isEmpty() && createIndex.isRequireMappings()) {
            throw new IllegalStateException("No mappings provided in " + this);
        }

        if (mappings.size() == 1 && mappings.containsKey(DOC)) {
            JsonNode node  =  Jackson2Mapper.getInstance().readTree(mappings.get(DOC).get());
            request.set("mappings", node);
        } else {
            ObjectNode mappingNode = request.with("mappings");
            for (Map.Entry<String, Supplier<String>> e : mappings.entrySet()) {
                mappingNode.set(e.getKey(), Jackson2Mapper.getInstance().readTree(e.getValue().get()));
            }
        }
        HttpEntity entity = entity(request);

        log.info("Creating index {} with mappings {}: {}", indexName, mappings.keySet(), request.toString());
        Request req = new Request("PUT", indexName);
        req.setEntity(entity);
        ObjectNode response = read(client().performRequest(req));


        if (response.get("acknowledged").booleanValue()) {
            log.info("Created index {}", getIndexName());
        } else {
            log.warn("Could not create index {}", getIndexName());
        }

    }

    @SneakyThrows
    public void reputSettings(boolean forReindex) {
        ObjectNode request = Jackson2Mapper.getInstance().createObjectNode();
        ObjectNode  settings = request.set("settings", Jackson2Mapper.getInstance().readTree(this.settings.get()));
        ObjectNode index = settings.with("settings").with("index");
        if (!index.has("refresh_interval")) {
            index.put("refresh_interval", "30s");
        }
        // remove stuff that cannot be updated
        index.remove("analysis");
        index.remove("number_of_shards");
        if (forReindex) {
            forReindex(settings);
        }
        HttpEntity entity = entity(request);
        Request req = new Request("PUT", getIndexName() + "/_settings");
        req.setEntity(entity);
        ObjectNode response = read(client().performRequest(req));

        log.info("{}", response);


    }

    protected void forReindex(ObjectNode  settings) {
        //https://www.elastic.co/guide/en/elasticsearch/reference/current/reindex-upgrade-remote.html
        ObjectNode index = settings.with("settings").with("index");
        index.put("refresh_interval", -1);
        index.put("number_of_replicas", 0);

    }


    @Override
    public boolean checkIndex() {
        try {
            Response response = client().performRequest(new Request("HEAD", getIndexName()));
            return response.getStatusLine().getStatusCode() != 404;
        } catch (IOException e) {
            log.error(e.getMessage(), e);
            return false;
        }
    }


    public void deleteIndex()  {
        try {
            Response delete = client().performRequest(new Request("DELETE", getIndexName()));
            log.info("{}", delete);
        } catch (IOException e) {
            log.error(e.getMessage(), e);
        }
    }

    public void clearIndex() {
        List<Pair<ObjectNode, ObjectNode>> bulk = new ArrayList<>();
        try (ElasticSearchIterator<JsonNode> i = ElasticSearchIterator.of(client())) {
            i.prepareSearch(getIndexName());

            while (i.hasNext()) {
                JsonNode node = i.next();
                bulk.add(deleteRequest(node.get(Fields.TYPE).asText(), node.get(Fields.ID).asText()));
            }
        }
        if (bulk.size() > 0) {
            bulk(bulk);
        }
    }


    public boolean refresh() {

        try {
            Response response = client().performRequest(new Request("GET", "_refresh"));
            JsonNode read = read(response);
            return read != null;
        } catch (IOException e) {
            log.error(e.getMessage(), e);
            return false;
        }
    }

    public String getVersionNumber() {

        try {
            Response response = client().performRequest(new Request("GET", ""));
            JsonNode read = read(response);
            return read.get("version").get("number").asText();
        } catch (IOException e) {
            log.error(e.getMessage(), e);
            return null;
        }
    }
    public Version<Integer> getVersion() {
        return Version.parseIntegers(getVersionNumber());
    }


    public ObjectNode search(ObjectNode request) {
        String indexName = indexNameSupplier == null ? null : indexNameSupplier.get();
        return post((indexName == null ? "" : indexName) + "/_search", request);
    }

    /**
     * @deprecated Types are deprecated in elasticsearch, and will disappear in 8.
     */
    @Deprecated
    public ObjectNode search(ObjectNode request, Enum<?>... types) {
        return search(request, Arrays.stream(types).map(Enum::name).toArray(String[]::new));
    }
    /**
     * @deprecated Types are deprecated in elasticsearch, and will disappear in 8.
     */
    @Deprecated
    public ObjectNode search(ObjectNode request, String... types) {
        String indexName = indexNameSupplier == null ? null : indexNameSupplier.get();
        StringBuilder path =  new StringBuilder((indexName == null ? "" : indexName));
        String typeString = String.join(",", types);
        if (typeString.length() > 0) {
            path.append("/").append(typeString);
        } else {
            //path.append("/_doc");
        }
        path.append("/_search");
        return post(path.toString(), request);
    }

    @SafeVarargs
    public final Future<ObjectNode> searchAsync(ObjectNode request, Consumer<ObjectNode>... listeners) {
        String indexName = indexNameSupplier == null ? null : indexNameSupplier.get();
        return postAsync((indexName == null ? "" : indexName) + "/_search", request, listeners);
    }


    public ObjectNode post(String path, ObjectNode request) {
        try {

            Request req = new Request("POST", path);
            req.setEntity(entity(request));
            log.info("Posting to {}", path);
            return read(client().performRequest(req));
        } catch (IOException e) {
            log.error(e.getMessage(), e);
            throw new RuntimeException(e);
        }
    }


    public static HttpEntity entity(JsonNode node) {
        return new NStringEntity(saveToString(node), ContentType.APPLICATION_JSON);
    }


    @SafeVarargs
    public final CompletableFuture<ObjectNode> postAsync(String path, ObjectNode request, Consumer<ObjectNode>... listeners) {
        final CompletableFuture<ObjectNode> future = new CompletableFuture<>();
        clientAsync((client) -> {
            log.debug("posting");
            Request req = new Request("POST", path);
            req.setEntity(entity(request));
            client.performRequestAsync(req, listen(log, request.toString(), future, listeners));
            }
        );
        return future;
    }

    @SafeVarargs
    static protected ResponseListener listen(
        Logger log,
        @NonNull final String requestDescription,
        @NonNull final CompletableFuture<ObjectNode> future,
        @NonNull  Consumer<ObjectNode>... listeners) {
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
                    ResponseException re = (ResponseException) exception;
                    Response response = re.getResponse();
                    ObjectNode result = read(log, response);
                    for (Consumer<ObjectNode> rl : listeners) {
                        rl.accept(result);
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
        return post(indexPath(id), objectMapper.valueToTree(o));
    }

    /**
     * @deprecated Types are deprecated in elasticsearch, and will disappear in 8.
     */
    @Deprecated
    public ObjectNode index(String type, String id, Object o) {
        return post(_indexPath(type, id, null), objectMapper.valueToTree(o));
    }
    /**
     * @deprecated Types are deprecated in elasticsearch, and will disappear in 8.
     */
    @Deprecated
    public ObjectNode index(String type, String id, Object o, String parent) {
        return post(_indexPath(type, id, parent), objectMapper.valueToTree(o));
    }

    public ObjectNode index(Pair<ObjectNode, ObjectNode> indexRequest) {
        return post(
            _indexPath(
                indexRequest.getFirst().get(TYPE).textValue(),
                indexRequest.getFirst().get(ID).textValue(),
                indexRequest.getFirst().get(PARENT).textValue()
            ),
            indexRequest.getSecond()
        );
    }

    /**
     * @deprecated Types are deprecated in elasticsearch, and will disappear in 8.
     */
    @SafeVarargs
    @Deprecated
    public final CompletableFuture<ObjectNode> indexAsync(String type, String id, Object o, Consumer<ObjectNode>... listeners) {
        return postAsync(getIndexName() + "/" + type + "/" + encode(id), objectMapper.valueToTree(o), listeners);
    }


    /**
     * @deprecated Types are deprecated in elasticsearch, and will disappear in 8.
     */
    @SafeVarargs
    @Deprecated
    public final Future<ObjectNode> indexAsync(String type, String id, Object o, String parent, Consumer<ObjectNode>... listeners) {
        return postAsync(_indexPath(type, id, parent), objectMapper.valueToTree(o), listeners);
    }



    protected String indexPath(String id) {
        return _indexPath(DOC, id, null);
    }

    /**
     * @deprecated Types are deprecated in elasticsearch, and will disappear in 8.
     */
    @Deprecated
    protected String indexPath(String type, String id, @Nullable  String parent) {
        return _indexPath(type, id, parent);
    }

    protected String _indexPath(String type, String id, String parent) {
        String path = getIndexName() + "/" + type + "/" + encode(id);
        if (parent != null) {
            path += "?parent=" + parent;
        }
        return path;
    }

    /**
     * @deprecated Types are deprecated in elasticsearch, and will disappear in 8.
     */
    @Deprecated
    public ObjectNode delete(String type, String id) {
        return _delete(type, id);
    }

    public ObjectNode delete(String id) {
        return _delete(DOC, id);
    }



    public ObjectNode _delete(String type, String id) {
        try {
            client().performRequest(new Request("DELETE", getIndexName() + "/" + type + "/" + encode(id)));
        } catch (IOException e) {
            log.error(e.getMessage(), e);
        }
        return null;
    }

    /**
     * @deprecated Types are deprecated in elasticsearch, and will disappear in 8.
     */
    @Deprecated
    public ObjectNode delete(String[] types, String id) {
        Collection<Pair<ObjectNode, ObjectNode>> bulkRequest = new ArrayList<>();
        for (String type : types) {
            bulkRequest.add(deleteRequest(type, id));
        }
        ObjectNode bulkResponse = bulk(bulkRequest);
        ObjectNode delete = null;
        for (JsonNode jsonNode : bulkResponse.withArray("items")) {
            delete = jsonNode.with("delete");
            if (delete.get("found").booleanValue()) {
                break;
            }
        }
        return delete;
    }


    public Future<ObjectNode> deleteAsync(Pair<ObjectNode, ObjectNode> deleteRequest, Consumer<ObjectNode>... listeners) {
        return deleteAsync(deleteRequest.getFirst().get(TYPE).textValue(), deleteRequest.getFirst().get(ID).textValue(), listeners);
    }


    /**
     * @deprecated Types are deprecated in elasticsearch, and will disappear in 8.
     */
    @SafeVarargs
    @Deprecated
    public final CompletableFuture<ObjectNode> deleteAsync(String type, String id, Consumer<ObjectNode>... listeners) {
        final CompletableFuture<ObjectNode> future = new CompletableFuture<>();

        client().performRequestAsync(new Request("DELETE", getIndexName() + "/" + type + "/" + encode(id)),
            listen(log, "delete " + type + "/" + id, future, listeners)
        );
        return future;
    }

    /**
     * @deprecated Types are deprecated in elasticsearch, and will disappear in 8.
     */
    @Deprecated
    public Optional<JsonNode> get(Enum<?> type, String id) {
        return _get(type.name(), id, null);
    }

    /**
     * @deprecated Types are deprecated in elasticsearch, and will disappear in 8.
     */
    @Deprecated
    public  Optional<JsonNode> get(String type, String id){
        return _get(type, id, null);
    }
    public  Optional<JsonNode> get(String id){
        return _get(DOC, id, null);
    }


    public  List<Optional<JsonNode>> mget(String... ids){
        if (ids.length == 0) {
            return Collections.emptyList();
        }
        Request get = new Request("GET", getIndexName() + "/_mget");
        ObjectNode body = Jackson2Mapper.getInstance().createObjectNode();
        ArrayNode array = body.withArray("ids");
        for (String id :ids ) {
            array.add(id);
        }
        get.setJsonEntity(saveToString(body));
        List<Optional<JsonNode>> result = new ArrayList<>();
        try {

            Response response = client().performRequest(get);
            ObjectNode objectNode = read(response);
            ArrayNode docs = objectNode.withArray("docs");

            for (JsonNode n : docs) {
                result.add(Optional.of(n));
            }
            return result;
        } catch (IOException e) {
            log.error(e.getMessage(), e);
        }
        return result;

    }


    public  Optional<JsonNode> getWithRouting(String id, String routing){
        return _get(DOC, id, routing);
    }

    protected Optional<JsonNode> _get(String type, String id, String routing) {
        try {
            Request get = new Request("GET", getIndexName() + "/" + type + "/" + encode(id));
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

    /**
     * @deprecated Types are deprecated in elasticsearch, and will disappear in 8.
     */
    @Deprecated
    public Optional<JsonNode> get(Collection<String> type, String id) {
        return get(type, id, (jn) -> jn);
    }

    /**
     * @deprecated Types are deprecated in elasticsearch, and will disappear in 8.
     */
    @Deprecated
    public <T> Optional<T> get(Collection<String> type, String id, Function<JsonNode, T> adapter) {
        return _get(type, id, adapter);
    }


    /**
     * @deprecated Types are deprecated in elasticsearch, and will disappear in 8.
     */
    public <T> Optional<T> _get(Collection<String> type, String id, Function<JsonNode, T> adapter) {
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


    /**
     * @deprecated Types are deprecated in elasticsearch, and will disappear in 8.
     */
    @Deprecated
    public Optional<JsonNode> getWithEnums(Collection<Enum<?>> type, String id) {
        return get(type.stream().map(Enum::name).collect(Collectors.toList()), id);
    }

    /**
     * @deprecated Types are deprecated in elasticsearch, and will disappear in 8.
     */
    @Deprecated
    public Optional<JsonNode> getSource(String type, String id) {
        return get(type, id).map(jn -> jn.get(Fields.SOURCE));
    }
    public Optional<JsonNode> getSource(String id) {
        return get(id).map(jn -> jn.get(Fields.SOURCE));
    }

    /**
     * @deprecated Types are deprecated in elasticsearch, and will disappear in 8.
     */
    @Deprecated
    public Optional<JsonNode> getSource(Enum<?> type, String id) {
        return get(type, id).map(jn -> jn.get(Fields.SOURCE));
    }

    public  ObjectNode read(Response response) {
        return read(log, response);

    }
    public static ObjectNode read(Logger log, Response response) {
        try {
            HttpEntity entity = response.getEntity();
            return Jackson2Mapper.getLenientInstance()
                .readerFor(ObjectNode.class)
                .readValue(entity.getContent());
        } catch (IOException e) {
            log.error(e.getMessage(), e);
            return null;
        }
    }

    String encode(String id) {
        try {
            return URLEncoder.encode(id, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            log.error(e.getMessage(), e);
            return id;
        }
    }

    /**
     * @deprecated Types are deprecated in elasticsearch, and will disappear in 8.
     */
    @Deprecated
    public Pair<ObjectNode, ObjectNode> indexRequest(String type, String id, Object o) {
        return _indexRequest(type, id, o);
    }
    public Pair<ObjectNode, ObjectNode> indexRequest(String id, Object o) {
        return _indexRequest(DOC, id, o);
    }

    public Pair<ObjectNode, ObjectNode> indexRequestWithRouting(String id, Object o, String routing) {
        Pair<ObjectNode, ObjectNode> request =
            _indexRequest(DOC, id, o);
        request.getFirst()
            .with(INDEX)
            .put(ROUTING, routing);
        return request;
    }

    private  Pair<ObjectNode, ObjectNode> _indexRequest(String type, String id, Object o) {
        ObjectNode actionLine = objectMapper.createObjectNode();
        ObjectNode index = actionLine.with(INDEX);
        if (! DOC.equals(type)) {
            index.put(Fields.TYPE, type);
        }
        index.put(Fields.ID, id);
        index.put(Fields.INDEX, getIndexName());

        ObjectNode jsonNode = objectMapper.valueToTree(o);
        return Pair.of(actionLine, jsonNode);
    }

    /**
     * @deprecated Types are deprecated in elasticsearch, and will disappear in 8.
     */
    @Deprecated
    public Pair<ObjectNode, ObjectNode> indexRequest(String type, String id, Object o, String routing) {
        Pair<ObjectNode, ObjectNode> request = indexRequest(type, id, o);
        request.getFirst().with(INDEX).put(Fields.ROUTING, routing);
        request.getFirst().with(INDEX).put(Fields.PARENT, routing);
        return request;
    }


    /**
     * @deprecated Types are deprecated in elasticsearch, and will disappear in 8.
     */
    @Deprecated
    public Pair<ObjectNode, ObjectNode> deleteRequest(String type, String id) {
        return _deleteRequest(type, id);
    }

    public Pair<ObjectNode, ObjectNode> deleteRequest(String id) {
        return _deleteRequest(DOC, id);
    }

    public Pair<ObjectNode, ObjectNode> deleteRequestWithRouting(String id, String routing) {
        Pair<ObjectNode, ObjectNode> request  = _deleteRequest(DOC, id);
        request.getFirst().with("delete")
            .put(ROUTING, routing);
        return request;

    }

    protected Pair<ObjectNode, ObjectNode> _deleteRequest(String type, String id) {
        ObjectNode actionLine = Jackson2Mapper.getInstance().createObjectNode();
        ObjectNode index = actionLine.with("delete");
        if (! DOC.equals(type)) {
            index.put(Fields.TYPE, type);
        }
        index.put(Fields.ID, id);
        index.put(Fields.INDEX, getIndexName());
        return Pair.of(actionLine, null);
    }
    /**
     * @deprecated Types are deprecated in elasticsearch, and will disappear in 8.
     */
    @Deprecated
    public Pair<ObjectNode, ObjectNode> deleteRequest(Enum<?> type, String id, String routing) {
        return deleteRequest(type.name(), id, routing);
    }
    /**
     * @deprecated Types are deprecated in elasticsearch, and will disappear in 8.
     */
    @Deprecated
    public Pair<ObjectNode, ObjectNode> deleteRequest(String type, String id, String routing) {
        Pair<ObjectNode, ObjectNode> request = deleteRequest(type, id);
        request.getFirst().with("delete").put(Fields.ROUTING, routing);
        return request;
    }


    public ObjectNode bulk(Collection<Pair<ObjectNode, ObjectNode>> request) {

        try {
            Request req = new Request("POST", "_bulk");
            req.setEntity(bulkEntity(request));

            writeJson(log, writeJsonDir, request);
            ObjectNode result = read(
                client().performRequest(req)
            );
            return result;
        } catch (IOException e) {
            log.error(e.getMessage(), e);
            return null;
        }
    }


    @SafeVarargs
    public final CompletableFuture<ObjectNode> bulkAsync(Collection<Pair<ObjectNode, ObjectNode>> request, Consumer<ObjectNode>... listeners) {
        return bulkAsync(log, writeJsonDir, client(), request, listeners);

    }


    @SafeVarargs
    public static CompletableFuture<ObjectNode> bulkAsync(Logger log, File jsonDir, RestClient client, Collection<Pair<ObjectNode, ObjectNode>> request, Consumer<ObjectNode>... listeners) {
        final CompletableFuture<ObjectNode> future = new CompletableFuture<>();
        if (request.size() == 0) {
            return CompletableFuture.completedFuture(null);
        }

        Request req = new Request("POST", "_bulk");
        req.setEntity(bulkEntity(request));
        writeJson(log, jsonDir, request);

        client.performRequestAsync(req,
            listen(log, "" + request.size() + " bulk operations", future, listeners)
        );
        return future;
    }

    protected static HttpEntity bulkEntity(Collection<Pair<ObjectNode, ObjectNode>> request) {
        StringBuilder builder = new StringBuilder();
        for (Pair<ObjectNode, ObjectNode> n : request) {
            builder.append(n.getFirst());
            builder.append("\n");
            if (n.getSecond() != null) {
                builder.append(saveToString(n.getSecond()));
                builder.append("\n");
            }
        }
        return new NStringEntity(builder.toString(), ContentType.APPLICATION_JSON);
    }

    static protected String saveToString(JsonNode jsonNode) {
        String value = jsonNode.toString();
        String replaced = value.replaceAll("\\p{Cc}", "");
        return replaced;

    }

    @Override
    public long count() {
        return count(new String[]{});
    }

    /**
     * @deprecated Types are deprecated in elasticsearch, and will disappear in 8.
     */
    @Deprecated
    public long count(String... types) {
        ObjectNode request = Jackson2Mapper.getInstance().createObjectNode();
        request.put("size", 0);
        ObjectNode response = search(request, types);
        log.info("Found {}", response);
        JsonNode jsonNode = response.get("hits").get("total");
        if (jsonNode.has("value")) {
            // es 7
            return jsonNode.get("value").longValue();
        } else {
            // es 5
            return jsonNode.longValue();
        }
    }

    /**
     * @deprecated Types are deprecated in elasticsearch, and will disappear in 8.
     */
    @Deprecated
    public long count(Enum<?>... types) {
        ObjectNode request = Jackson2Mapper.getInstance().createObjectNode();
        request.put("size", 0);
        ObjectNode response = search(request, types);
        return response.get("hits").get("total").longValue();
    }


    public void countAsync(final Consumer<Long> consumer) {
        ObjectNode request = Jackson2Mapper.getInstance().createObjectNode();
        request.put("size", 0);
        searchAsync(request, (entity) ->  {
            JsonNode hits = entity.get(HITS);
            if (hits != null) {
                long count = hits.get("total").longValue();
                consumer.accept(count);
            } else {
                log.warn("{}", entity);
                consumer.accept(null);
            }
        });
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
        } catch (InterruptedException | ExecutionException e) {
            throw new RuntimeException(e);
        }
    }


    @SafeVarargs
    public final CompletableFuture<String> getClusterNameAsync(Consumer<String>... callBacks) {
        final RestClient client = client();
        return getClusterName(log, client, callBacks);

    }

    @SafeVarargs
    public static CompletableFuture<String> getClusterName(Logger log, RestClient client, final Consumer<String>... callBacks) {
        final CompletableFuture<String> future = new CompletableFuture<>();
        client.performRequestAsync(new Request("GET", "/_cat/health"), new ResponseListener() {
            @Override
            public void onSuccess(Response response) {
                ByteArrayOutputStream out = new ByteArrayOutputStream();
                try {
                    IOUtils.copy(response.getEntity().getContent(), out);
                    String[] content = out.toString("UTF-8").split("\\s+");
                    log.info("Found {}", Arrays.asList(content));
                    String clusterName = content[2];
                    try {
                        for (Consumer<String> callBack : callBacks) {
                            callBack.accept(clusterName);
                        }
                    } catch (Exception e) {
                        future.completeExceptionally(e);
                        return;
                    }
                    future.complete(clusterName);
                } catch (IOException e) {
                    log.error(e.getMessage(), e);
                    future.completeExceptionally(e);
                }
            }

            @Override
            public void onFailure(Exception exception) {
                log.error("Error getting clustername from {}: {}", client, exception.getMessage(), exception);
                future.completeExceptionally(exception);
            }

        });
        return  future;
    }


    public Consumer<ObjectNode> indexLogger(Logger logger) {
        return indexLogger(logger, () -> "");
    }

    public Consumer<ObjectNode> indexLogger(Logger logger, Supplier<String> prefix) {
        return jsonNode -> {
            String index = jsonNode.get(Fields.INDEX).textValue();
            String type = jsonNode.get(Fields.TYPE).textValue();
            String id = jsonNode.get(Fields.ID).textValue();
            Integer version = jsonNode.hasNonNull(Fields.VERSION) ? jsonNode.get(Fields.VERSION).intValue() : null;
            logger.info("{}{}/{}/{}/{} version: {}", prefix.get(), clientFactory, index, type, encode(id), version);
            logger.debug("{}{}", prefix.get(), jsonNode);
        };
    }


    public Consumer<ObjectNode> deleteLogger(Logger logger) {
        return deleteLogger(logger, () -> "");
    }


    public Consumer<ObjectNode> deleteLogger(Logger logger, Supplier<String> prefix) {
        return jsonNode -> {
            boolean found = jsonNode.has("found") && jsonNode.get("found").booleanValue();
            String index = jsonNode.get(Fields.INDEX).textValue();
            String type = jsonNode.get(Fields.TYPE).textValue();
            String id = jsonNode.get(Fields.ID).textValue();
            if (found) {
                int version = jsonNode.has(Fields.VERSION) ? jsonNode.get(Fields.VERSION).intValue() : -1;
                logger.info("{}{}/{}/{}/{} version: {}", prefix.get(), clientFactory, index, type, encode(id), version);
            } else {
                logger.info("{}{}/{}/{}/{} (not found)", prefix.get(), clientFactory, index, type, encode(id));
            }
            logger.debug("{}{} {}", prefix.get(), clientFactory, jsonNode);
        };
    }


    public Consumer<ObjectNode> bulkLogger(Logger indexLog, Logger deleteLog) {
        @SuppressWarnings("MismatchedQueryAndUpdateOfStringBuilder")
        StringBuilder logPrefix = new StringBuilder();
        Consumer<ObjectNode> indexLogger = indexLogger(indexLog, logPrefix::toString);
        Consumer<ObjectNode> deleteLogger = deleteLogger(deleteLog, logPrefix::toString);

        return jsonNode -> {
            ArrayNode items = jsonNode.withArray("items");
            int i = 0;
            int total = items.size();
            for (JsonNode n : items) {
                logPrefix.setLength(0);
                logPrefix.append(++i).append('/').append(total).append(' ');
                ObjectNode on = (ObjectNode) n;
                boolean recognized = false;
                if (on.has("delete")) {
                    deleteLogger.accept(on.with("delete"));
                    recognized = true;
                }
                if (n.has("index")) {
                    indexLogger.accept(on.with("index"));
                    recognized = true;
                }
                if (! recognized) {
                    log.warn("{}Unrecognized bulk response {}", logPrefix, n);
                }

            }
        };
    }

    public Consumer<ObjectNode> bulkLogger(Logger logger) {
        return jsonNode -> {
            ArrayNode items = jsonNode.withArray("items");
            String index = null;
            String type = null;
            List<String> deleted = new ArrayList<>();
            List<String> indexed = new ArrayList<>();
            for (JsonNode n : items) {
                ObjectNode on = (ObjectNode) n;
                if (on.has("delete")) {
                    ObjectNode delete = on.with("delete");
                    index = delete.get(Fields.INDEX).textValue();
                    type = delete.get(Fields.TYPE).textValue();
                    String id = delete.get(Fields.ID).textValue();
                    deleted.add(type+ ":" + id);
                    continue;
                }
                if (n.has("index")) {
                    ObjectNode indexResponse = on.with("index");
                    index = indexResponse.get(Fields.INDEX).textValue();
                    type = indexResponse.get(Fields.TYPE).textValue();
                    String id = indexResponse.get(Fields.ID).textValue();
                    indexed.add(type + ":" + id);
                    continue;
                }
                logger.warn("Unrecognized bulk response {}", n);

            }

            logger.info("{} {}/{} indexed: {}, revoked: {}", clientFactory, index, type, indexed, deleted);
        };
    }

    public void setWriteJsonDir(File file) {
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

    @Override
    public String toString() {
        return "IndexHelper{" +
            "indexName=" + indexNameSupplier.get() +
            ", aliases=" + aliases +
            ", writeJsonDir=" + writeJsonDir +
            ", elasticSearchIndex=" + elasticSearchIndex +
            '}';
    }

    static protected void writeJson(Logger log, File writeJsonDir, Collection<Pair<ObjectNode, ObjectNode>> requests) {
        for (Pair<ObjectNode, ObjectNode> request: requests) {
            ObjectNode actionLine = request.getFirst();
            if (actionLine.has("index")) {
                writeJson(log, writeJsonDir, actionLine.get("index").get(Fields.ID).textValue(), request.getSecond());
            }
        }
    }

    static protected void writeJson(Logger log, File  writeJsonDir, String id, JsonNode jsonNode) {
        if (writeJsonDir != null) {
            File file = new File(writeJsonDir, id.replaceAll(File.separator, "_") + ".json");
            try {
                Jackson2Mapper.getPrettyInstance().writeValue(new FileOutputStream(file), jsonNode);
                log.info("Wrote {}", file);
            } catch (IOException e) {
                log.error(e.getMessage(), e);
            }
        }
    }

}
