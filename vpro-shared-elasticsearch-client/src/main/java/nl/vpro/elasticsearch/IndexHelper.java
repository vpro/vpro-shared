package nl.vpro.elasticsearch;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.function.Supplier;

import org.apache.commons.io.IOUtils;
import org.apache.http.HttpEntity;
import org.apache.http.entity.ContentType;
import org.apache.http.nio.entity.NStringEntity;
import org.elasticsearch.client.Response;
import org.elasticsearch.client.RestClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;

import nl.vpro.jackson2.Jackson2Mapper;

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

    public  void createIndex() throws ExecutionException, InterruptedException, IOException {

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
        ObjectNode response = Jackson2Mapper.getLenientInstance().readerFor(ObjectNode.class).readValue(
            client().performRequest("PUT", indexNameSupplier.get(), Collections.emptyMap(), entity).getEntity().getContent());


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


    public void deleteIndex() throws ExecutionException, InterruptedException, IOException {
        client().performRequest("DELETE", getIndexName());
    }

    public boolean refresh() throws ExecutionException, InterruptedException, IOException {
        Response response = client().performRequest("GET", "_refresh");
        return response != null;
    }

    public ObjectNode search(ObjectNode request) {
        return post(indexNameSupplier.get() + "/_search", request);
    }


    public ObjectNode post(String path, ObjectNode request) {
        try {
            return
            Jackson2Mapper.getLenientInstance().readerFor(ObjectNode.class).readValue(
                client().performRequest("POST", path, Collections.emptyMap(), new NStringEntity(request.toString(), ContentType.APPLICATION_JSON))
                    .getEntity().getContent()
            );
        } catch (IOException e) {
            log.error(e.getMessage(), e);
            return null;
        }
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


}
