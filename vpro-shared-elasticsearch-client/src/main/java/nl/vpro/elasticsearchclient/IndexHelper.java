package nl.vpro.elasticsearchclient;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import org.apache.commons.io.IOUtils;
import org.apache.http.HttpEntity;
import org.apache.http.entity.ContentType;
import org.apache.http.nio.entity.NStringEntity;
import org.elasticsearch.client.Response;
import org.elasticsearch.client.ResponseException;
import org.elasticsearch.client.ResponseListener;
import org.elasticsearch.client.RestClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.util.concurrent.Futures;

import nl.vpro.jackson2.Jackson2Mapper;
import nl.vpro.util.Pair;

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
        Map<String,
            Supplier<String>> mappings) {
        this.log = log == null ? LoggerFactory.getLogger(IndexHelper.class) : log;
        this.clientFactory = client;
        this.indexNameSupplier = indexNameSupplier == null ? () -> "" : indexNameSupplier;
        this.settings = settings;
        if (mappings != null) {
            this.mappings.putAll(mappings);
        }
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
            return Futures.immediateFuture(client);
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
        ObjectNode response = read(client().performRequest("PUT", indexNameSupplier.get(), Collections.emptyMap(), entity));


        if (response.get("acknowledged").booleanValue()) {
            log.info("Created index {}", getIndexName());
        } else {
            log.warn("Could not create index {}", getIndexName());
        }

    }


    public void prepareIndex() {
        try {

            Response response = client().performRequest("HEAD",  getIndexName());
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
            Response response = client().performRequest("HEAD", getIndexName());
            return response.getStatusLine().getStatusCode() != 404;
        } catch (IOException e) {
            log.error(e.getMessage(), e);
            return false;
        }
    }


    public void deleteIndex()  {
        try {
            client().performRequest("DELETE", getIndexName());
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
            bulk.add(deleteRequest(node.get("_type").asText(), node.get("_id").asText()));
        }
        bulk(bulk);
    }

    public boolean refresh() {

        try {
            Response response = client().performRequest("GET", "_refresh");
            JsonNode read = read(response);
            return response != null;
        } catch (IOException e) {
            log.error(e.getMessage(), e);
            return false;
        }
    }

    public ObjectNode search(ObjectNode request, String... types) {
        String indexName = indexNameSupplier == null ? null : indexNameSupplier.get();
        StringBuilder path =  new StringBuilder((indexName == null ? "" : indexName));
        String typeString = Arrays.stream(types).collect(Collectors.joining(","));
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

            return read(client().performRequest("POST", path, Collections.emptyMap(), entity(request)));
        } catch (IOException e) {
            log.error(e.getMessage(), e);
            throw new RuntimeException(e);
        }
    }


    HttpEntity entity(JsonNode node) {
        return new NStringEntity(saveToString(node), ContentType.APPLICATION_JSON);
    }


    @SafeVarargs
    public final Future<ObjectNode> postAsync(String path, ObjectNode request, Consumer<ObjectNode>... listeners) {
        final CompletableFuture<ObjectNode> future = new CompletableFuture<>();
        clientAsync((client) -> {
            log.debug("posting");
            client.performRequestAsync("POST", path, Collections.emptyMap(), entity(request), listen(future, listeners));
            }
        );
        return future;
    }

    @SafeVarargs
    protected final ResponseListener listen(final CompletableFuture<ObjectNode> future, Consumer<ObjectNode>... listeners) {
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
                log.error(exception.getMessage(), exception);
                future.completeExceptionally(exception);

            }
        };
    }

    public ObjectNode index(String type, String id, Object o) {
        return post(indexPath(type, id, null), getPublisherInstance().valueToTree(o));
    }


    public ObjectNode index(String type, String id, Object o, String parent) {
        return post(indexPath(type, id, parent), getPublisherInstance().valueToTree(o));
    }

    public ObjectNode index(Pair<ObjectNode, ObjectNode> indexRequest) {
        return post(indexPath(indexRequest.getFirst().get("type").textValue(), indexRequest.getFirst().get("id").textValue(), indexRequest.getFirst().get("parent").textValue()), indexRequest.getSecond());
    }

    @SafeVarargs
    public final Future<ObjectNode> indexAsync(String type, String id, Object o, Consumer<ObjectNode>... listeners) {
        return postAsync(getIndexName() + "/" + type + "/" + encode(id), getPublisherInstance().valueToTree(o), listeners);
    }


    @SafeVarargs
    public final Future<ObjectNode> indexAsync(String type, String id, Object o, String parent, Consumer<ObjectNode>... listeners) {
        return postAsync(indexPath(type, id, parent), getPublisherInstance().valueToTree(o));
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
            client().performRequest("DELETE", getIndexName() + "/" + type + "/" + encode(id));
        } catch (IOException e) {
            log.error(e.getMessage(), e);
        }
        return null;
    }


    public Future<ObjectNode> deleteAsync(Pair<ObjectNode, ObjectNode> deleteRequest, Consumer<ObjectNode>... listeners) {
        return deleteAsync(deleteRequest.getFirst().get("type").textValue(), deleteRequest.getFirst().get("id").textValue(), listeners);
    }


    @SafeVarargs
    public final Future<ObjectNode> deleteAsync(String type, String id, Consumer<ObjectNode>... listeners) {
        final CompletableFuture<ObjectNode> future = new CompletableFuture<>();

        client().performRequestAsync("DELETE", getIndexName() + "/" + type + "/" + encode(id), listen(future, listeners));
        return future;
    }

    public Optional<JsonNode> get(String type, String id){
        try {
            Response response = client()
                .performRequest("GET", getIndexName() + "/" + type + "/" + encode(id));
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


    public Optional<JsonNode> getSource(String type, String id) {
        return get(type, id).map(jn -> jn.get("_source"));
    }

    public Optional<JsonNode> get(Collection<String> types, String id) {
        return types.stream().map(t -> get(t, id)).filter(Optional::isPresent).map(Optional::get).findFirst();
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
        ObjectNode index = actionLine.with("index");
        index.put("_type", type);
        index.put("_id", id);
        index.put("_index", getIndexName());

        ObjectNode jsonNode = getPublisherInstance().valueToTree(o);
        return Pair.of(actionLine, jsonNode);
    }

    public Pair<ObjectNode, ObjectNode> indexRequest(String type, String id, Object o, String routing) {
        Pair<ObjectNode, ObjectNode> request = indexRequest(type, id, o);
        request.getFirst().with("index").put("_routing", routing);
        request.getFirst().with("index").put("_parent", routing);
        return request;
    }


    public Pair<ObjectNode, ObjectNode> deleteRequest(String type, String id) {
        ObjectNode actionLine = Jackson2Mapper.getInstance().createObjectNode();
        ObjectNode index = actionLine.with("delete");
        index.put("_type", type);
        index.put("_id", id);
        index.put("_index", getIndexName());
        return Pair.of(actionLine, null);
    }

    public Pair<ObjectNode, ObjectNode> deleteRequest(String type, String id, String routing) {
        Pair<ObjectNode, ObjectNode> request = deleteRequest(type, id);
        request.getFirst().with("delete").put("_routing", routing);
        return request;
    }


    public ObjectNode bulk(Collection<Pair<ObjectNode, ObjectNode>> request) {
        try {
            ObjectNode result = read(
                client().performRequest(
                    "POST", "_bulk",
                    Collections.emptyMap(),
                    bulkEntity(request))
            );
            return result;
        } catch (IOException e) {
            log.error(e.getMessage(), e);
            return null;
        }
    }


    @SafeVarargs
    public final Future<ObjectNode> bulkAsync(Collection<Pair<ObjectNode, ObjectNode>> request, Consumer<ObjectNode>... listeners) {
        final CompletableFuture<ObjectNode> future = new CompletableFuture<>();

        client().performRequestAsync("POST", "_bulk", Collections.emptyMap(), bulkEntity(request), listen(future, listeners));
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


    public long count(String... types) {
        ObjectNode request = Jackson2Mapper.getInstance().createObjectNode();
        request.put("size", 0);
        ObjectNode response = search(request, types);
        return response.get("hits").get("total").longValue();
    }


    public void countAsync(final Consumer<Long> consumer) {
        ObjectNode request = Jackson2Mapper.getInstance().createObjectNode();
        request.put("size", 0);
        searchAsync(request, (entity) ->  {
            long count = entity.get("hits").get("total").longValue();
            consumer.accept(count);
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
            return getClusterNameAsync((s) -> {
            }).get();
        } catch (InterruptedException | ExecutionException e) {
            throw new RuntimeException(e);
        }
    }


    public Future<String> getClusterNameAsync(Consumer<String> callBack) {
        CompletableFuture<String> future = new CompletableFuture<>();
        client().performRequestAsync("GET", "/", Collections.emptyMap(), new ResponseListener() {
            @Override
            public void onSuccess(Response response) {
                ObjectNode node = read(response);
                log.info("Found {}", node);
                String clusterName;
                if (node.has("cluster_name")) {
                    clusterName = node.get("cluster_name").asText();
                } else {
                    log.warn("Could not found cluster_name in {} with {}", node, client());
                    clusterName = null;
                }
                callBack.accept(clusterName);
                future.complete(clusterName);

            }

            @Override
            public void onFailure(Exception exception) {
                future.completeExceptionally(exception);
            }

        });
        return  future;
    }


}
