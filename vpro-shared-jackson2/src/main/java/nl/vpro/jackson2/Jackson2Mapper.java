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
import com.fasterxml.jackson.databind.Module;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.introspect.AnnotationIntrospectorPair;
import com.fasterxml.jackson.databind.introspect.JacksonAnnotationIntrospector;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.module.jaxb.JaxbAnnotationIntrospector;

/**
 * @author Rico
 * @author Michiel Meeuwissen
 */
@Slf4j
public class Jackson2Mapper extends ObjectMapper {

    private static boolean loggedAboutAvro = false;

    public static Jackson2Mapper INSTANCE = new Jackson2Mapper();
    public static Jackson2Mapper LENIENT = new Jackson2Mapper();
    public static Jackson2Mapper STRICT = new Jackson2Mapper();
    public static Jackson2Mapper PRETTY = new Jackson2Mapper();
    public static Jackson2Mapper PUBLISHER = new Jackson2Mapper();
    public static Jackson2Mapper PRETTY_PUBLISHER = new Jackson2Mapper();


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

    @SneakyThrows({JsonProcessingException.class})
    public static <T> T lenientTreeToValue(JsonNode jsonNode, Class<T> clazz) {
        return getLenientInstance().treeToValue(jsonNode, clazz);
    }

    private Jackson2Mapper() {
        configureMapper(this);

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
        mapper.enable(JsonParser.Feature.ALLOW_COMMENTS);
        mapper.enable(JsonParser.Feature.ALLOW_UNQUOTED_FIELD_NAMES);
        mapper.enable(JsonParser.Feature.ALLOW_NUMERIC_LEADING_ZEROS);
        mapper.enable(MapperFeature.USE_WRAPPER_NAME_AS_PROPERTY_NAME);

        JavaTimeModule javaTimeModule = new JavaTimeModule();
        mapper.registerModule(javaTimeModule);
        mapper.registerModule(new DateModule());

        mapper.setConfig(mapper.getSerializationConfig().withView(Views.Normal.class));
        mapper.setConfig(mapper.getDeserializationConfig().withView(Views.Normal.class));


        try {
            Class<?> avro = Class.forName("nl.vpro.jackson2.SerializeAvroModule");
            mapper.registerModule((Module) avro.newInstance());
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
}
