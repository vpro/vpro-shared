package nl.vpro.elasticsearch;


import lombok.*;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.function.*;

import org.apache.commons.io.IOUtils;
import org.checkerframework.checker.nullness.qual.NonNull;

import com.fasterxml.jackson.databind.JsonNode;

import nl.vpro.jackson2.Jackson2Mapper;


/**
 * @author Michiel Meeuwissen
 * @since 2.9
 */
@lombok.Getter()
public class ElasticSearchIndex {

    private final String indexName;
    private final String settingsResource;
    private final String mappingResource;
    private final List<String> aliases;

    private static final Consumer<JsonNode> NOP_MAPPER = (jn) -> {};

    @With
    @NonNull
    private final Consumer<JsonNode> mappingsProcessor;

    protected ElasticSearchIndex(
        String indexName,
        String mappingResource,
        String... aliases) {
        this(indexName, "/es7/setting/" + indexName + ".json", mappingResource, Arrays.asList(aliases), null);
    }

    @lombok.Builder(builderClassName = "Builder")
    protected ElasticSearchIndex(
        String indexName,
        String settingsResource,
        String mappingResource,
        @lombok.Singular  List<String> aliases,
        Consumer<JsonNode> mappingsProcessor) {
        this.indexName = indexName;
        this.settingsResource = settingsResource;
        this.mappingResource = mappingResource;
        this.aliases = aliases;
        this.mappingsProcessor = mappingsProcessor == null ? NOP_MAPPER: mappingsProcessor;
    }


    public Supplier<JsonNode> settings() {
        return () -> resourceToJson(settingsResource);
    }

    public Supplier<JsonNode> mapping() {
        return () -> resourceToJson(mappingResource);
    }

    public ElasticSearchIndex withoutExperimental() {
        return this;
    }

    /**
     * Registers a mapping processor while leaving the existing one intact.
     */
    public ElasticSearchIndex thenWithMappingsProcessor(Consumer<JsonNode> mappingsProcessor) {
        return withMappingsProcessor(this.mappingsProcessor.andThen(mappingsProcessor));
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

    @Override
    public String toString() {
        return getClass().getSimpleName() + ":" + getIndexName() + (aliases.isEmpty() ? "" : " " + aliases);
    }


    public static String resourceToString(String name) {
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

    @SneakyThrows
    public static JsonNode resourceToJson(String name) {
        return Jackson2Mapper.getInstance().readTree(resourceToString(name));
    }


    public static class Builder {

        public Builder publishAlias() {
            if (indexName == null) {
                throw new IllegalStateException();
            }
            return alias(indexName + "-publish");
        }
    }
}
