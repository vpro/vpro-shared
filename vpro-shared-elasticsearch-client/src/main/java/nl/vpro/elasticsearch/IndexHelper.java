package nl.vpro.elasticsearch;

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
import java.util.concurrent.Future;
import java.util.function.Supplier;

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

import nl.vpro.jackson2.Jackson2Mapper;
import nl.vpro.util.Pair;

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
    private IndexHelper(Logger log, ESClientFactory client, Supplier<String> indexNameSupplier, Supplier<String> settings, Map<String, Supplier<String>> mappings) {
        this.log = log == null ? LoggerFactory.getLogger(IndexHelper.class) : log;
        this.clientFactory = client;
        this.indexNameSupplier = indexNameSupplier;
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
        return clientFactory.client(IndexHelper.class.getName() + "." + indexNameSupplier.get());
    }

    public  void createIndex() throws IOException {

        if (indexNameSupplier == null){
            throw new IllegalStateException("No index name configured");
        }
        ObjectNode request = Jackson2Mapper.getInstance().createObjectNode();
        request.set("settings", Jackson2Mapper.getInstance().readTree(settings.get()));
        ObjectNode mappingNode = request.with("mappings");


        for (Map.Entry<String, Supplier<String>> e : mappings.entrySet()) {
            mappingNode.set(e.getKey(), Jackson2Mapper.getInstance().readTree(e.getValue().get()));
        }
        HttpEntity entity = new NStringEntity(request.toString(), ContentType.APPLICATION_JSON);

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
        ObjectNode request = i.prepareSearch(getIndexName());
        List<Pair<JsonNode, JsonNode>> bulk = new ArrayList<>();
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

    public ObjectNode search(ObjectNode request) {
        return post(indexNameSupplier.get() + "/_search", request);
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
        return new NStringEntity(node.toString(), ContentType.APPLICATION_JSON);
    }


    public Future<ObjectNode> postAsync(String path, ObjectNode request) {
        final CompletableFuture<ObjectNode> future = new CompletableFuture<>();
        client().performRequestAsync("POST", path, Collections.emptyMap(), new NStringEntity(request.toString(), ContentType.APPLICATION_JSON), listen(future));
        return future;
    }

    protected ResponseListener listen(final CompletableFuture<ObjectNode> future) {
        return new ResponseListener() {
            @Override
            public void onSuccess(Response response) {
                future.complete(read(response));
            }

            @Override
            public void onFailure(Exception exception) {
                future.completeExceptionally(exception);
            }
        };
    }

    public ObjectNode index(String type, String id, Object o) {
        return post(indexNameSupplier.get() + "/" + type + "/" + encode(id), Jackson2Mapper.getPublisherInstance().valueToTree(o));
    }

    public Future<ObjectNode> indexAsync(String type, String id, Object o) {
        return postAsync(indexNameSupplier.get() + "/" + type + "/" + encode(id), Jackson2Mapper.getPublisherInstance().valueToTree(o));
    }


    public ObjectNode delete(String type, String id) {
        try {
            client().performRequest("DELETE", indexNameSupplier.get() + "/" + type + "/" + encode(id));
        } catch (IOException e) {
            log.error(e.getMessage(), e);
        }
        return null;
    }


    public Future<ObjectNode> deleteAsync(String type, String id) {
        final CompletableFuture<ObjectNode> future = new CompletableFuture<>();

        client().performRequestAsync("DELETE", indexNameSupplier.get() + "/" + type + "/" + encode(id), listen(future));
        return future;
    }

    public Optional<JsonNode> get(String types, String id){
        try {
            Response response = client().performRequest("GET", indexNameSupplier.get() + "/" + types + "/" + encode(id));
            return Optional.of(read(response));
        } catch (ResponseException re) {
            return Optional.empty();
        } catch (IOException e) {
            log.error(e.getMessage(), e);
            throw new RuntimeException(e);
        }

    }


    ObjectNode read(Response response) {
        try {
            HttpEntity entity = response.getEntity();
            return Jackson2Mapper.getLenientInstance().readerFor(ObjectNode.class).readValue(entity.getContent());
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

    public Pair<JsonNode, JsonNode> indexRequest(String type, String id, Object o) {
        ObjectNode actionLine = Jackson2Mapper.getInstance().createObjectNode();
        ObjectNode index = actionLine.with("index");
        index.put("_type", type);
        index.put("_id", id);
        index.put("_index", getIndexName());
        return Pair.of(actionLine, Jackson2Mapper.getPublisherInstance().valueToTree(o));
    }

    public Pair<JsonNode, JsonNode> deleteRequest(String type, String id) {
        ObjectNode actionLine = Jackson2Mapper.getInstance().createObjectNode();
        ObjectNode index = actionLine.with("delete");
        index.put("_type", type);
        index.put("_id", id);
        index.put("_index", getIndexName());
        return Pair.of(actionLine, null);
    }

    public ObjectNode bulk(List<Pair<JsonNode, JsonNode>> request) {
        try {
            return read(
                client().performRequest("POST", "_bulk", Collections.emptyMap(), bulkEntity(request))
            );
        } catch (IOException e) {
            log.error(e.getMessage(), e);
            return null;
        }
    }


    public Future<ObjectNode> bulkAsync(List<Pair<JsonNode, JsonNode>> request) {
        final CompletableFuture<ObjectNode> future = new CompletableFuture<>();

        client().performRequestAsync("POST", "_bulk", Collections.emptyMap(), bulkEntity(request), listen(future));
        return future;
    }

    protected HttpEntity bulkEntity(List<Pair<JsonNode, JsonNode>> request) {
        StringBuilder builder = new StringBuilder();
        for (Pair<JsonNode, JsonNode> n : request) {
            builder.append(n.getFirst());
            builder.append("\n");
            if (n.getSecond() != null) {
                builder.append(n.getSecond());
                builder.append("\n");
            }
        }
        return new NStringEntity(builder.toString(), ContentType.APPLICATION_JSON);
    }

    public long count() {
        ObjectNode request = Jackson2Mapper.getInstance().createObjectNode();
        request.put("size", 0);
        ObjectNode response = search(request);
        return response.get("hits").get("total").longValue();
    }


    public void setIndexName(String indexName) {
        this.indexNameSupplier = () -> indexName;
    }


    public String getIndexName() {
        return indexNameSupplier.get();
    }

    public String getClusterName() {
        try {
            ObjectNode node = read(client().performRequest("GET", "/", Collections.emptyMap()));
            return node.get("cluster_name").asText();
        } catch (IOException e) {
            log.error(e.getMessage(), e);
            return null;
        }
    }


}
