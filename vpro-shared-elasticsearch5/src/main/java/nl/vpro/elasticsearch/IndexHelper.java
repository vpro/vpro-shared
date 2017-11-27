package nl.vpro.elasticsearch;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.function.Supplier;

import org.elasticsearch.ResourceAlreadyExistsException;
import org.elasticsearch.action.admin.indices.create.CreateIndexRequestBuilder;
import org.elasticsearch.action.admin.indices.create.CreateIndexResponse;
import org.elasticsearch.action.admin.indices.refresh.RefreshResponse;
import org.elasticsearch.client.Client;
import org.elasticsearch.client.transport.NoNodeAvailableException;
import org.elasticsearch.common.xcontent.XContentType;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
    private String indexName;
    private Supplier<String> settings;
    private ESClientFactory clientFactory;
    private final Map<String, Supplier<String>> mappings = new HashMap<>();

    public static class Builder {
        private final Map<String, Supplier<String>> mappings = new HashMap<>();

        public Builder mapping(String type, Supplier<String> mapping) {
            this.mappings.put(type, mapping);
            return this;
        }
        public Builder mappings(Map<String, Supplier<String>> mappings) {
            this.mappings.putAll(mappings);
            return this;
        }
    }


    @lombok.Builder(builderClassName = "Builder")
    private IndexHelper(Logger log, ESClientFactory client, String indexName, Supplier<String> settings, Map<String, Supplier<String>> mappings) {
        this.log = log == null ? LoggerFactory.getLogger(IndexHelper.class) : log;
        this.clientFactory = client;
        this.indexName = indexName;
        this.settings = settings;
        if (mappings != null) {
            this.mappings.putAll(mappings);
        }
    }

    public IndexHelper mapping(String type, Supplier<String> mapping) {
        mappings.put(type, mapping);
        return this;
    }

    public Client client() {
        return clientFactory.client(IndexHelper.class.getName() + "." + indexName);
    }

    public  void createIndex() throws ExecutionException, InterruptedException, IOException {

        if (indexName == null){
            throw new IllegalStateException("No index name configured");
        }
        try {
            CreateIndexRequestBuilder createIndexRequestBuilder = client().admin().indices()
                .prepareCreate(indexName)
                .setSettings(settings.get(), XContentType.JSON);
            for (Map.Entry<String, Supplier<String>> e : mappings.entrySet()) {
                createIndexRequestBuilder.addMapping(e.getKey(), e.getValue().get(), XContentType.JSON);
            }
            log.info("Creating index {} with mappings {}", indexName, mappings.keySet());
            CreateIndexResponse response = createIndexRequestBuilder.execute()
                .actionGet();
            if (response.isAcknowledged()) {
                log.info("Created index {}", indexName);
            } else {
                log.warn("Could not create index {}", indexName);
            }
        } catch (ResourceAlreadyExistsException e) {
            log.info("Index exists");
        }
    }


    public void prepareIndex() {
        try {
            boolean exists = client().admin().indices().prepareExists(indexName).execute().actionGet().isExists();
            if (!exists) {
                log.info("Index '{}' not existing in {}, now creating", indexName, clientFactory);
                try {
                    createIndex();
                } catch (Exception e) {
                    String c = (e.getCause() != null ? (" " + e.getCause().getMessage()) : "");
                    log.error(e.getMessage() + c);
                }
            } else {
                log.info("Found {} objects in '{}' of {}", count(), indexName, clientFactory);
            }
        } catch( NoNodeAvailableException noNodeAvailableException) {
            log.error(noNodeAvailableException.getMessage());
        }
    }


    public void deleteIndex() throws ExecutionException, InterruptedException, IOException {
        client().admin().indices().prepareDelete(indexName).execute().actionGet();
    }

    public RefreshResponse refresh() throws ExecutionException, InterruptedException {
        return client().admin().indices().prepareRefresh(indexName).get();
    }

    public long count() {
        return client().prepareSearch(indexName).setSource(new SearchSourceBuilder().size(0)).get().getHits().getTotalHits();
    }


}
