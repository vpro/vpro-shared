package nl.vpro.elasticsearch7;

import lombok.*;

import java.util.HashMap;
import java.util.Map;
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

import nl.vpro.elasticsearch.ElasticSearchIndex;
import nl.vpro.elasticsearch.IndexHelperInterface;

import static nl.vpro.elasticsearch.ElasticSearchIndex.resourceToString;

/**
 * Some tools to automaticly create indices and put mappings and stuff.
 * @author Michiel Meeuwissen
 * @since 0.24
 */
@ToString
@Getter
@Setter
public class IndexHelper implements IndexHelperInterface<Client> {

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

    public static IndexHelper.Builder of(Logger log, ESClientFactory client, Supplier<String> indexName, String objectType) {
        return IndexHelper.builder().log(log)
            .client(client)
            .indexNameSupplier(indexName)
            .settingsResource("es/setting.json")
            .mappingResource(objectType, String.format("es/%s.json", objectType));
    }

    public static IndexHelper.Builder of(Logger log, ESClientFactory client, ElasticSearchIndex index) {
        return IndexHelper.builder()
            .log(log)
            .client(client)
            .indexNameSupplier(index::getIndexName)
            .settingsResource(index.getSettingsResource())
            .mappings(index.mappingsAsMap());
    }

    public IndexHelper mapping(String type, Supplier<String> mapping) {
        mappings.put(type, mapping);
        return this;
    }

    @Override
    public Client client() {
        return clientFactory.client(IndexHelper.class.getName() + "." + indexNameSupplier.get());
    }

    @Override
    public  void createIndex() {

        if (indexNameSupplier == null){
            throw new IllegalStateException("No index name configured");
        }
        try {
            String indexName = indexNameSupplier.get();
            CreateIndexRequestBuilder createIndexRequestBuilder = client().admin().indices()
                .prepareCreate(indexName)
                .setSettings(settings.get(), XContentType.JSON);
            for (Map.Entry<String, Supplier<String>> e : mappings.entrySet()) {
                createIndexRequestBuilder.addMapping(e.getKey(), e.getValue().get(), XContentType.JSON);
            }
            log.debug("Creating index {} with mappings {}", indexName, mappings.keySet());
            CreateIndexResponse response = createIndexRequestBuilder.execute()
                .actionGet();
            if (response.isAcknowledged()) {
                log.info("Created index {} with mappings {}", indexName, mappings.keySet());
            } else {
                log.warn("Could not create index {}", indexName);
            }
        } catch (ResourceAlreadyExistsException e) {
            log.info("Index exists {}", e.getMessage());
        }
    }


    public void prepareIndex() {
        try {
            boolean exists = client().admin().indices().prepareExists(getIndexName()).execute().actionGet().isExists();
            if (!exists) {
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
        } catch( NoNodeAvailableException noNodeAvailableException) {
            log.error(noNodeAvailableException.getMessage());
        }
    }


    public void deleteIndex() {
        client().admin().indices().prepareDelete(getIndexName()).execute().actionGet();
    }

    public RefreshResponse refresh() {
        return client().admin().indices().prepareRefresh(getIndexName()).get();
    }

    public long count() {
        return client().prepareSearch(getIndexName()).setSource(new SearchSourceBuilder().size(0)).get().getHits().getTotalHits().value;
    }


    public void setIndexName(String indexName) {
        this.indexNameSupplier = () -> indexName;
    }


    public String getIndexName() {
        return indexNameSupplier.get();
    }


}
