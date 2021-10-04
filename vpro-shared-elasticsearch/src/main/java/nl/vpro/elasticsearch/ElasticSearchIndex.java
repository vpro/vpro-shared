package nl.vpro.elasticsearch;


import lombok.SneakyThrows;
import lombok.With;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.function.*;

import org.apache.commons.io.IOUtils;
import org.checkerframework.checker.nullness.qual.NonNull;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import nl.vpro.jackson2.Jackson2Mapper;

import static nl.vpro.elasticsearch.Constants.Mappings.PROPERTIES;


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

    public static final BiConsumer<Distribution, ObjectNode> DEFAULT_MAPPER = new BiConsumer<Distribution, ObjectNode>() {
        @Override
        public void accept(Distribution distribution, ObjectNode jsonNode) {
            ObjectNode properties =  jsonNode.with(PROPERTIES);
            List<Consumer<ObjectNode>> runnable = new ArrayList<>();
            properties.fieldNames().forEachRemaining(f -> {
                String[] split = f.split("\\|", 2);
                if (split.length == 2) {
                    runnable.add(p -> {
                        JsonNode copy = p.get(f);
                        Distribution forDistro = Distribution.valueOf(split[1].toUpperCase());
                        p.remove(f);
                        if (forDistro == distribution) {
                            p.set(split[0], copy);
                        }
                    });
                }
            });
            runnable.forEach(c -> c.accept(properties));
        }

        @Override
        public String toString() {
            return "DEFAULT";
        }

    };

    @With
    @NonNull
    private final BiConsumer<Distribution, ObjectNode>  mappingsProcessor;

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
        BiConsumer<Distribution, ObjectNode> mappingsProcessor) {
        this.indexName = indexName;
        this.settingsResource = settingsResource;
        this.mappingResource = mappingResource;
        this.aliases = aliases;
        this.mappingsProcessor = mappingsProcessor == null ? DEFAULT_MAPPER: DEFAULT_MAPPER.andThen(mappingsProcessor);
    }


    public Supplier<ObjectNode> settings() {
        return () -> resourceToObjectNode(settingsResource);
    }

    public Supplier<ObjectNode> mapping() {
        return () -> resourceToObjectNode(mappingResource);
    }

    public ElasticSearchIndex withoutExperimental() {
        return this;
    }

    /**
     * Registers a mapping processor while leaving the existing one intact.
     */
    public ElasticSearchIndex thenWithMappingsProcessor(BiConsumer<Distribution, JsonNode> mappingsProcessor) {
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

    public static ObjectNode resourceToObjectNode(String name) {
        return (ObjectNode) resourceToJson(name);
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
