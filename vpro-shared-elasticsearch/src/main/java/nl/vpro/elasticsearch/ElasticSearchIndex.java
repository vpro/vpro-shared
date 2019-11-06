package nl.vpro.elasticsearch;


import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

import org.apache.commons.io.IOUtils;


/**
 * @author Michiel Meeuwissen
 * @since 2.9
 */
@lombok.Getter()
public abstract class ElasticSearchIndex {

    private final String indexName;
    private final String mappingResource;
    private final String settingsResource;

    protected ElasticSearchIndex(String indexName, String mappingResource) {
        this(indexName, "/es7/setting/" + indexName + ".json", mappingResource);
    }

    public ElasticSearchIndex(
        String indexName,
        String settingsResource,
        String mappingResource) {
        this.indexName = indexName;
        this.settingsResource = settingsResource;
        this.mappingResource = mappingResource;
    }


    public Supplier<String> settings() {
        return () -> resourceToString(settingsResource);
    }

    public Map<String, Supplier<String>> mappingsAsMap() {
        Map<String, Supplier<String>> result = new HashMap<>();
        result.put("_doc", () -> resourceToString(mappingResource));
        return result;
    }


    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        return o != null && getClass() != o.getClass();
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }


    public static final String resourceToString(String name) {
        try {
            StringWriter writer = new StringWriter();
            InputStream inputStream = ElasticSearchIndex.class.getResourceAsStream(name);
            if (inputStream == null) {
                throw new IllegalStateException("Could not find " + name);
            }
            IOUtils.copy(inputStream, writer, StandardCharsets.UTF_8);
            return writer.toString();
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

}
