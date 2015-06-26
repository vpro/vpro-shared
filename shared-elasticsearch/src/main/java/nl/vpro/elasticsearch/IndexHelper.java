package nl.vpro.elasticsearch;

import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutionException;

import org.apache.commons.io.IOUtils;
import org.elasticsearch.action.admin.indices.create.CreateIndexResponse;
import org.elasticsearch.action.admin.indices.mapping.put.PutMappingResponse;
import org.elasticsearch.action.admin.indices.refresh.RefreshResponse;
import org.elasticsearch.client.Client;
import org.elasticsearch.client.transport.NoNodeAvailableException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Some tools to automaticly create indices and put mappings and stuff.
 * @author Michiel Meeuwissen
 * @since 0.24
 */
public class IndexHelper {

    private static final Logger LOG = LoggerFactory.getLogger(IndexHelper.class);


    private String indexName;
    private String settings;
    private ESClientFactory client;
    private final Map<String, String> mappings = new HashMap<>();


    public IndexHelper(ESClientFactory client, String indexName, String settings) {
        this.client = client;
        this.indexName = indexName;
        this.settings = settings;
    }

    public IndexHelper mapping(String type, String mapping) {
        mappings.put(type, mapping);
        return this;
    }

    private Client client() {
        return client.buildClient(IndexHelper.class.getSimpleName() + "." + indexName);
    }

    public  void createIndex() throws ExecutionException, InterruptedException, IOException {
        CreateIndexResponse response = client().admin().indices().prepareCreate(indexName).setSettings(read(settings)).execute().actionGet();
        if (response.isAcknowledged()) {
            LOG.info("Created index {}", indexName);
        } else {
            LOG.warn("Could not create index {}", indexName);
        }
        putMappings();
    }

    public void putMappings() throws ExecutionException, InterruptedException, IOException {
        for (Map.Entry<String, String> e : mappings.entrySet()) {
            PutMappingResponse a = client().admin().indices().preparePutMapping(indexName).setType(e.getKey()).setSource(read(e.getValue())).execute().actionGet();
            if (a.isAcknowledged()) {
                LOG.info("Put mapping {}/{}", indexName, e.getKey());
            } else {
                LOG.warn("Could not put mapping {}/", indexName, e.getKey());
            }
        }
    }

    public void prepareIndex() {
        try {
            boolean exists = client().admin().indices().prepareExists(indexName).execute().actionGet().isExists();
            if (!exists) {
                LOG.info("Index '{}' not existing in {}, now creating", indexName, client);
                try {
                    createIndex();
                } catch (Exception e) {
                    String c = (e.getCause() != null ? (" " + e.getCause().getMessage()) : "");
                    LOG.error(e.getMessage() + c);
                }
            } else {
                try {
                    putMappings();
                } catch (Exception e) {
                    String c = (e.getCause() != null ? (" " + e.getCause().getMessage()) : "");
                    LOG.error(e.getMessage() + c);

                }
                LOG.info("Found {} tips in '{}' of {}", count(), indexName, client);
            }
        } catch( NoNodeAvailableException noNodeAvailableException) {
            LOG.error(noNodeAvailableException.getMessage());
        }
    }


    public void deleteIndex() throws ExecutionException, InterruptedException, IOException {
        client().admin().indices().prepareDelete(indexName).execute().actionGet();
    }

    public RefreshResponse refresh() throws ExecutionException, InterruptedException {
        return client().admin().indices().prepareRefresh(indexName).get();
    }

    public long count() {
        return client().prepareCount(indexName).get().getCount();
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
