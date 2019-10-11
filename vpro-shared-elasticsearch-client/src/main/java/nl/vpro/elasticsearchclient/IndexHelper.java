package nl.vpro.elasticsearchclient;

import lombok.*;

import java.io.*;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.*;
import java.util.stream.Collectors;

import org.apache.commons.io.IOUtils;
import org.apache.http.HttpEntity;
import org.apache.http.entity.ContentType;
import org.apache.http.nio.entity.NStringEntity;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.elasticsearch.client.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import nl.vpro.jackson2.Jackson2Mapper;
import nl.vpro.util.Pair;
import nl.vpro.util.Version;

import static nl.vpro.elasticsearchclient.Constants.Fields;
import static nl.vpro.elasticsearchclient.Constants.HITS;
import static nl.vpro.jackson2.Jackson2Mapper.getPublisherInstance;

/**
 * Some tools to automaticly create indices and put mappings and stuff.
 * @author Michiel Meeuwissen
 * @since 0.24
 */
@ToString
@Getter
@Setter
public class IndexHelper {

    private final Logger log;
    private Supplier<String> indexNameSupplier;
    private Supplier<String> settings;
    private ESClientFactory clientFactory;
    private final Map<String, Supplier<String>> mappings = new HashMap<>();
    private ObjectMapper objectMapper;

    private File writeJsonDir = null;


    public static class Builder {
        private final Map<String, Supplier<String>> mappings = new HashMap<>();

        public Builder mapping(String type, Supplier<String> mapping) {
            this.mappings.put(type, mapping);
            return this;
        }



        public Builder mappingResource(String type, String mapping) {
            return mapping(type, () -> getResourceAsString(mapping));
        }

        public Builder mappingResource(String mapping) {
            String[] split = mapping.split("/");
            String fileName = split[split.length - 1];
            int dot = fileName.lastIndexOf(".");
            String type = fileName.substring(0, dot);
            return mapping(type, () -> getResourceAsString(mapping));
        }
        public Builder mappings(Map<String, Supplier<String>> mappings) {
            this.mappings.putAll(mappings);
            return this;
        }

        public Builder settingsResource(final String resource) {
            return settings(() -> getResourceAsString(resource)
            );
        }

        public Builder indexName(final String indexName) {
            return indexNameSupplier(() -> indexName);
        }
    }

    private static String getResourceAsString(String resource) {
        try {
            StringWriter e = new StringWriter();
            InputStream inputStream = IndexHelper.class.getClassLoader().getResourceAsStream(resource);
            if (inputStream == null) {
                throw new IllegalStateException("Could not find " + resource);
            } else {
                IOUtils.copy(inputStream, e, StandardCharsets.UTF_8);
                return e.toString();

            }
        } catch (IOException var3) {
            throw new IllegalStateException(var3);
        }
    }


    @lombok.Builder(builderClassName = "Builder")
    private IndexHelper(
        Logger log,
        ESClientFactory client,
        Supplier<String> indexNameSupplier,
        Supplier<String> settings,
        Map<String, Supplier<String>> mappings,
        File writeJsonDir,
        ObjectMapper objectMapper
        ) {
        this.log = log == null ? LoggerFactory.getLogger(IndexHelper.class) : log;
        this.clientFactory = client;
        this.indexNameSupplier = indexNameSupplier == null ? () -> "" : indexNameSupplier;
        this.settings = settings;
        if (mappings != null) {
            this.mappings.putAll(mappings);
        }
        this.writeJsonDir = writeJsonDir;
        this.objectMapper = objectMapper == null ? getPublisherInstance() : objectMapper;
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

    public IndexHelper mapping(String type, Supplier<String> mapping) {
        mappings.put(type, mapping);
        return this;
    }

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

    public  void createIndex() throws IOException {

        if (getIndexName().isEmpty()){
            throw new IllegalStateException("No index name configured");
        }
        ObjectNode request = Jackson2Mapper.getInstance().createObjectNode();
        request.set("settings", Jackson2Mapper.getInstance().readTree(settings.get()));
        ObjectNode mappingNode = request.with("mappings");


        for (Map.Entry<String, Supplier<String>> e : mappings.entrySet()) {
            mappingNode.set(e.getKey(), Jackson2Mapper.getInstance().readTree(e.getValue().get()));
        }
        HttpEntity entity = entity(request);

        log.info("Creating index {} with mappings {}: {}", indexNameSupplier, mappings.keySet(), request.toString());
        Request req = new Request("PUT", indexNameSupplier.get());
        req.setEntity(entity);
        ObjectNode response = read(client().performRequest(req));


        if (response.get("acknowledged").booleanValue()) {
            log.info("Created index {}", getIndexName());
        } else {
            log.warn("Could not create index {}", getIndexName());
        }

    }


    /**
     * Checks whether index exists, and if not, created it.
     */
    public void createIndexIfNotExists() {
        try {
            Response response = client().performRequest(new Request("HEAD",  getIndexName()));
            if (response.getStatusLine().getStatusCode() == 404) {
                log.info("Index '{}' not existing in {}, now creating", getIndexName(), clientFactory);
                try {
                    createIndex();
                } catch (Exception e) {
                    String c = (e.getCause() != null ? (" " + e.getCause().getMessage()) : "");
                    log.error(e.getMessage() + c);
                }
            } else {
                log.info("Found {} objects in '{}' of {}", count(), getIndexName(), clientFactory);
            }
        } catch( IOException noNodeAvailableException) {
            log.error(noNodeAvailableException.getMessage());
        }
    }

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
            client().performRequest(new Request("DELETE", getIndexName()));
        } catch (IOException e) {
            log.error(e.getMessage(), e);
        }
    }

    public void clearIndex() {
        ElasticSearchIterator<JsonNode> i = ElasticSearchIterator.of(client());
        i.prepareSearch(Collections.singleton(getIndexName()), null);
        List<Pair<ObjectNode, ObjectNode>> bulk = new ArrayList<>();
        while (i.hasNext()) {
            JsonNode node = i.next();
            bulk.add(deleteRequest(node.get(Fields.TYPE).asText(), node.get(Fields.ID).asText()));
        }
        if (bulk.size() > 0) {
            bulk(bulk);
        }
    }


    public boolean refresh() {

        try {
            Response response = client().performRequest(new Request("GET", "_refresh"));
            JsonNode read = read(response);
            return response != null;
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
        return search(request, new String[] {});
    }
    public ObjectNode search(ObjectNode request, Enum<?>... types) {
        return search(request, Arrays.stream(types).map(Enum::name).toArray(String[]::new));
    }
    public ObjectNode search(ObjectNode request, String... types) {
        String indexName = indexNameSupplier == null ? null : indexNameSupplier.get();
        StringBuilder path =  new StringBuilder((indexName == null ? "" : indexName));
        String typeString = String.join(",", types);
        if (typeString.length() > 0) {
            path.append("/").append(typeString);
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
            return read(client().performRequest(req));
        } catch (IOException e) {
            log.error(e.getMessage(), e);
            throw new RuntimeException(e);
        }
    }




    HttpEntity entity(JsonNode node) {
        return new NStringEntity(saveToString(node), ContentType.APPLICATION_JSON);
    }


    @SafeVarargs
    public final CompletableFuture<ObjectNode> postAsync(String path, ObjectNode request, Consumer<ObjectNode>... listeners) {
        final CompletableFuture<ObjectNode> future = new CompletableFuture<>();
        clientAsync((client) -> {
            log.debug("posting");
            Request req = new Request("POST", path);
            req.setEntity(entity(request));
            client.performRequestAsync(req, listen(request.toString(), future, listeners));
            }
        );
        return future;
    }

    @SafeVarargs
    protected final ResponseListener listen(
        @NonNull final String requestDescription,
        @NonNull final CompletableFuture<ObjectNode> future,
        @NonNull  Consumer<ObjectNode>... listeners) {
        return new ResponseListener() {
            @Override
            public void onSuccess(Response response) {
                ObjectNode result = read(response);
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
                    ObjectNode result = read(response);
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

    public ObjectNode index(String type, String id, Object o) {
        return post(indexPath(type, id, null), objectMapper.valueToTree(o));
    }


    public ObjectNode index(String type, String id, Object o, String parent) {
        return post(indexPath(type, id, parent), objectMapper.valueToTree(o));
    }

    public ObjectNode index(Pair<ObjectNode, ObjectNode> indexRequest) {
        return post(
            indexPath(
                indexRequest.getFirst().get(Constants.TYPE).textValue(),
                indexRequest.getFirst().get(Constants.ID).textValue(),
                indexRequest.getFirst().get(Constants.PARENT).textValue()
            ),
            indexRequest.getSecond()
        );
    }

    @SafeVarargs
    public final CompletableFuture<ObjectNode> indexAsync(String type, String id, Object o, Consumer<ObjectNode>... listeners) {
        return postAsync(getIndexName() + "/" + type + "/" + encode(id), objectMapper.valueToTree(o), listeners);
    }


    @SafeVarargs
    public final Future<ObjectNode> indexAsync(String type, String id, Object o, String parent, Consumer<ObjectNode>... listeners) {
        return postAsync(indexPath(type, id, parent), objectMapper.valueToTree(o), listeners);
    }


    protected String indexPath(String type, String id, String parent) {
        String path = getIndexName() + "/" + type + "/" + encode(id);
        if (parent != null) {
            path += "?parent=" + parent;
        }
        return path;
    }


    public ObjectNode delete(String type, String id) {
        try {
            client().performRequest(new Request("DELETE", getIndexName() + "/" + type + "/" + encode(id)));
        } catch (IOException e) {
            log.error(e.getMessage(), e);
        }
        return null;
    }

    public ObjectNode delete(String[] types, String id) {
        Collection<Pair<ObjectNode, ObjectNode>> bulkRequest = new ArrayList<>();
        for (String type : types) {
            bulkRequest.add(deleteRequest(type, id));
        }
        ObjectNode bulkResponse = bulk(bulkRequest);
        ObjectNode delete = null;
        for (JsonNode jsonNode : bulkResponse.withArray("items")) {
            delete = (ObjectNode) jsonNode.with("delete");
            if (delete.get("found").booleanValue()) {
                break;
            }
        }
        return delete;
    }


    public Future<ObjectNode> deleteAsync(Pair<ObjectNode, ObjectNode> deleteRequest, Consumer<ObjectNode>... listeners) {
        return deleteAsync(deleteRequest.getFirst().get(Constants.TYPE).textValue(), deleteRequest.getFirst().get(Constants.ID).textValue(), listeners);
    }


    @SafeVarargs
    public final CompletableFuture<ObjectNode> deleteAsync(String type, String id, Consumer<ObjectNode>... listeners) {
        final CompletableFuture<ObjectNode> future = new CompletableFuture<>();

        client().performRequestAsync(new Request("DELETE", getIndexName() + "/" + type + "/" + encode(id)),
            listen("delete " + type + "/" + id, future, listeners)
        );
        return future;
    }

    public Optional<JsonNode> get(Enum<?> type, String id) {
        return get(type.name(), id);
    }
    public Optional<JsonNode> get(String type, String id){
        try {
            Response response = client()
                .performRequest(new Request("GET", getIndexName() + "/" + type + "/" + encode(id)));
            return Optional.of(read(response));
        } catch (ResponseException re) {
            if (re.getResponse().getStatusLine().getStatusCode() >= 500) {
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

    public Optional<JsonNode> get(Collection<String> type, String id) {
        return get(type, id, (jn) -> jn);
    }


    public <T> Optional<T> get(Collection<String> type, String id, Function<JsonNode, T> adapter) {
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

    public Optional<JsonNode> getWithEnums(Collection<Enum<?>> type, String id) {
        return get(type.stream().map(Enum::name).collect(Collectors.toList()), id);
    }

    public Optional<JsonNode> getSource(String type, String id) {
        return get(type, id).map(jn -> jn.get(Fields.SOURCE));
    }

    public Optional<JsonNode> getSource(Enum<?> type, String id) {
        return get(type, id).map(jn -> jn.get(Fields.SOURCE));
    }

    public ObjectNode read(Response response) {
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

    public Pair<ObjectNode, ObjectNode> indexRequest(String type, String id, Object o) {
        ObjectNode actionLine = Jackson2Mapper.getInstance().createObjectNode();
        ObjectNode index = actionLine.with(Constants.INDEX);
        index.put(Fields.TYPE, type);
        index.put(Fields.ID, id);
        index.put(Fields.INDEX, getIndexName());

        ObjectNode jsonNode = objectMapper.valueToTree(o);
        return Pair.of(actionLine, jsonNode);
    }

    public Pair<ObjectNode, ObjectNode> indexRequest(String type, String id, Object o, String routing) {
        Pair<ObjectNode, ObjectNode> request = indexRequest(type, id, o);
        request.getFirst().with(Constants.INDEX).put(Fields.ROUTING, routing);
        request.getFirst().with(Constants.INDEX).put(Fields.PARENT, routing);
        return request;
    }


    public Pair<ObjectNode, ObjectNode> deleteRequest(String type, String id) {
        ObjectNode actionLine = Jackson2Mapper.getInstance().createObjectNode();
        ObjectNode index = actionLine.with("delete");
        index.put(Fields.TYPE, type);
        index.put(Fields.ID, id);
        index.put(Fields.INDEX, getIndexName());
        return Pair.of(actionLine, null);
    }

    public Pair<ObjectNode, ObjectNode> deleteRequest(Enum<?> type, String id, String routing) {
        return deleteRequest(type.name(), id, routing);
    }
    public Pair<ObjectNode, ObjectNode> deleteRequest(String type, String id, String routing) {
        Pair<ObjectNode, ObjectNode> request = deleteRequest(type, id);
        request.getFirst().with("delete").put(Fields.ROUTING, routing);
        return request;
    }


    public ObjectNode bulk(Collection<Pair<ObjectNode, ObjectNode>> request) {

        try {
            Request req = new Request("POST", "_bulk");
            req.setEntity(bulkEntity(request));

            writeJson(request);
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
        final CompletableFuture<ObjectNode> future = new CompletableFuture<>();
        if (request.size() == 0) {
            return CompletableFuture.completedFuture(null);
        }

        Request req = new Request("POST", "_bulk");
        req.setEntity(bulkEntity(request));
        writeJson(request);

        client().performRequestAsync(req,
            listen("" + request.size() + " bulk operations", future, listeners)
        );
        return future;
    }

    protected HttpEntity bulkEntity(Collection<Pair<ObjectNode, ObjectNode>> request) {
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

    protected String saveToString(JsonNode jsonNode) {
        String value = jsonNode.toString();
        String replaced = value.replaceAll("\\p{Cc}", "");
        return replaced;

    }

    public long count() {
        return count(new String[]{});
    }

    public long count(String... types) {
        ObjectNode request = Jackson2Mapper.getInstance().createObjectNode();
        request.put("size", 0);
        ObjectNode response = search(request, types);
        return response.get("hits").get("total").longValue();
    }

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
            int version = jsonNode.get("_version").intValue();
            if (found) {
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

    protected void writeJson(Collection<Pair<ObjectNode, ObjectNode>> requests) {
        for (Pair<ObjectNode, ObjectNode> request: requests) {
            ObjectNode actionLine = request.getFirst();
            if (actionLine.has("index")) {
                writeJson(actionLine.get("index").get(Fields.ID).textValue(), request.getSecond());
            }
        }
    }

    protected void writeJson(String id, JsonNode jsonNode) {
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
