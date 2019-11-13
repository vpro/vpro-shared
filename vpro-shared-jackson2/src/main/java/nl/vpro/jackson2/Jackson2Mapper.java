/*
 * Copyright (C) 2012 All rights reserved
 * VPRO The Netherlands
 */
package nl.vpro.jackson2;

import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.json.JsonReadFeature;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.introspect.AnnotationIntrospectorPair;
import com.fasterxml.jackson.databind.introspect.JacksonAnnotationIntrospector;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.module.jaxb.JaxbAnnotationIntrospector;

/**
 * @author Rico
 * @author Michiel Meeuwissen
 */
@Slf4j
public class Jackson2Mapper extends ObjectMapper {

    private static boolean loggedAboutAvro = false;

    public static final Jackson2Mapper INSTANCE = new Jackson2Mapper("instance");
    public static final Jackson2Mapper LENIENT = new Jackson2Mapper("lenient");
    public static final Jackson2Mapper STRICT = new Jackson2Mapper("strict");
    public static final Jackson2Mapper PRETTY = new Jackson2Mapper("pretty");
    public static final Jackson2Mapper PUBLISHER = new Jackson2Mapper("publisher");
    public static final Jackson2Mapper PRETTY_PUBLISHER = new Jackson2Mapper("pretty_publisher");

    private static ThreadLocal<Jackson2Mapper> THREAD_LOCAL = ThreadLocal.withInitial(() -> INSTANCE);


    static {
        LENIENT.enable(DeserializationFeature.READ_UNKNOWN_ENUM_VALUES_AS_NULL);
        STRICT.enable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
        PRETTY.enable(SerializationFeature.INDENT_OUTPUT);

        PUBLISHER.setConfig(PUBLISHER.getSerializationConfig().withView(Views.Publisher.class));
        PUBLISHER.setConfig(PUBLISHER.getDeserializationConfig().withView(Views.Normal.class));

        PRETTY_PUBLISHER.setConfig(PUBLISHER.getSerializationConfig().withView(Views.Publisher.class));
        PRETTY_PUBLISHER.setConfig(PUBLISHER.getDeserializationConfig().withView(Views.Normal.class));
        PRETTY_PUBLISHER.enable(SerializationFeature.INDENT_OUTPUT);


        //PRETTY.enable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES); // This gives quite a lot of troubles. Though I'd like it to be set, especailly because PRETTY is used in tests.
    }

    public static Jackson2Mapper getInstance() {
        return INSTANCE;
    }

    public static Jackson2Mapper getLenientInstance() {
        return LENIENT;
    }

    public static Jackson2Mapper getPrettyInstance() {
        return PRETTY;
    }


    public static Jackson2Mapper getPublisherInstance() {
        return PUBLISHER;
    }


    public static Jackson2Mapper getPrettyPublisherInstance() {
        return PRETTY_PUBLISHER;
    }

    public static Jackson2Mapper getThreadLocal() {
        return THREAD_LOCAL.get();
    }
    public static void setThreadLocal(Jackson2Mapper set) {
        THREAD_LOCAL.set(set);
    }

    @SneakyThrows({JsonProcessingException.class})
    public static <T> T lenientTreeToValue(JsonNode jsonNode, Class<T> clazz) {
        return getLenientInstance().treeToValue(jsonNode, clazz);
    }

    private final String toString;

    private Jackson2Mapper(String toString) {
        configureMapper(this);
        this.toString = toString;

    }


    public static void configureMapper(ObjectMapper mapper) {
         AnnotationIntrospector introspector = new AnnotationIntrospectorPair(
            new JacksonAnnotationIntrospector(),
            new JaxbAnnotationIntrospector(mapper.getTypeFactory()
            ));

        mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
        mapper.setSerializationInclusion(JsonInclude.Include.NON_EMPTY);
        mapper.setAnnotationIntrospector(introspector);

        mapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES); // This seems a good idea when reading from couchdb or so, but when reading user supplied forms, it is confusing not getting errors.

        mapper.enable(JsonParser.Feature.ALLOW_SINGLE_QUOTES);
        mapper.enable(JsonParser.Feature.ALLOW_UNQUOTED_FIELD_NAMES);
        mapper.enable(MapperFeature.USE_WRAPPER_NAME_AS_PROPERTY_NAME);

        mapper.setConfig(mapper.getDeserializationConfig().with(JsonReadFeature.ALLOW_LEADING_ZEROS_FOR_NUMBERS));
        mapper.setConfig(mapper.getDeserializationConfig().with(JsonReadFeature.ALLOW_JAVA_COMMENTS));

        mapper.registerModule(new JavaTimeModule());
        mapper.registerModule(new DateModule());
        // For example normal support for Optional.
        Jdk8Module jdk8Module = new Jdk8Module();
        jdk8Module.configureAbsentsAsNulls(true);
        mapper.registerModule(jdk8Module);

        mapper.setConfig(mapper.getSerializationConfig().withView(Views.Normal.class));
        mapper.setConfig(mapper.getDeserializationConfig().withView(Views.Normal.class));


        try {
            Class<?> avro = Class.forName("nl.vpro.jackson2.SerializeAvroModule");
            mapper.registerModule((com.fasterxml.jackson.databind.Module) avro.newInstance());
        } catch (ClassNotFoundException ncdfe) {
            if (! loggedAboutAvro) {
                log.debug("SerializeAvroModule could not be registered because: " + ncdfe.getClass().getName() + " " + ncdfe.getMessage());
            }
            loggedAboutAvro = true;
        } catch (IllegalAccessException | InstantiationException e) {
            log.error(e.getMessage(), e);
            loggedAboutAvro = true;
        }
    }

    @Override
    public String toString() {
        return Jackson2Mapper.class.getSimpleName() + " (" + toString + ")";
    }
}
