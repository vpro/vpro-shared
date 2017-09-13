package nl.vpro.elasticsearch;

import lombok.ToString;

import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutionException;

import org.apache.commons.io.IOUtils;
import org.elasticsearch.ResourceAlreadyExistsException;
import org.elasticsearch.action.admin.indices.create.CreateIndexRequestBuilder;
import org.elasticsearch.action.admin.indices.create.CreateIndexResponse;
import org.elasticsearch.action.admin.indices.refresh.RefreshResponse;
import org.elasticsearch.client.Client;
import org.elasticsearch.client.transport.NoNodeAvailableException;
import org.elasticsearch.common.xcontent.XContentType;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.slf4j.Logger;

/**
 * Some tools to automaticly create indices and put mappings and stuff.
 * @author Michiel Meeuwissen
 * @since 0.24
 */
@ToString
public class IndexHelper {

    private final Logger log;


    private String indexName;
    private String settings;
    private ESClientFactory client;
    private final Map<String, String> mappings = new HashMap<>();


    public IndexHelper(Logger log, ESClientFactory client, String indexName, String settings) {
        this.log = log;
        this.client = client;
        this.indexName = indexName;
        this.settings = settings;
    }

    public IndexHelper mapping(String type, String mapping) {
        mappings.put(type, mapping);
        return this;
    }

    public static IndexHelper of(Logger log, ESClientFactory client, String indexName, String objectType) {
        return new IndexHelper(log, client, indexName, "es/setting.json").mapping(objectType, String.format("es/%s.json", objectType));
    }

    private Client client() {
        return client.client(IndexHelper.class.getName() + "." + indexName);
    }

    public  void createIndex() throws ExecutionException, InterruptedException, IOException {

        try {
            CreateIndexRequestBuilder createIndexRequestBuilder = client().admin().indices()
                .prepareCreate(indexName)
                .setSettings(settings, XContentType.JSON);
            for (Map.Entry<String, String> e : mappings.entrySet()) {
                createIndexRequestBuilder.addMapping(e.getKey(), e.getValue(), XContentType.JSON);
            }
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
        CreateIndexResponse response = client().admin().indices().prepareCreate(indexName).setSettings(read(settings), XContentType.JSON).execute().actionGet();
    }


    public void prepareIndex() {
        try {
            boolean exists = client().admin().indices().prepareExists(indexName).execute().actionGet().isExists();
            if (!exists) {
                log.info("Index '{}' not existing in {}, now creating", indexName, client);
                try {
                    createIndex();
                } catch (Exception e) {
                    String c = (e.getCause() != null ? (" " + e.getCause().getMessage()) : "");
                    log.error(e.getMessage() + c);
                }
            } else {
                log.info("Found {} objects in '{}' of {}", count(), indexName, client);
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


    public String getIndexName() {
        return indexName;
    }

    public void setIndexName(String indexName) {
        this.indexName = indexName;
    }

    public String getSettings() {
        return settings;
    }

    public void setSettings(String settings) {
        this.settings = settings;
    }

    public ESClientFactory getClient() {
        return client;
    }

    public void setClient(ESClientFactory client) {
        this.client = client;
    }

    private static String read(String source) throws IOException {
        StringWriter writer = new StringWriter();
        InputStream input = IndexHelper.class.getClassLoader().getResourceAsStream(source);
        if (input == null) {
            throw new IllegalStateException("Could not find " + source);
        }
        IOUtils.copy(input, writer, "utf-8");
        return writer.toString();
    }
}
