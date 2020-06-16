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
    public static final Jackson2Mapper PRETTY_STRICT = new Jackson2Mapper("pretty_strict");

    public static final Jackson2Mapper PRETTY = new Jackson2Mapper("pretty");
    public static final Jackson2Mapper PUBLISHER = new Jackson2Mapper("publisher");
    public static final Jackson2Mapper PRETTY_PUBLISHER = new Jackson2Mapper("pretty_publisher");
    public static final Jackson2Mapper BACKWARDS_PUBLISHER = new Jackson2Mapper("backwards_publisher");

    private static final ThreadLocal<Jackson2Mapper> THREAD_LOCAL = ThreadLocal.withInitial(() -> INSTANCE);


    static {
        LENIENT.enable(DeserializationFeature.READ_UNKNOWN_ENUM_VALUES_AS_NULL);
        STRICT.enable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
        PRETTY.enable(SerializationFeature.INDENT_OUTPUT);
        PRETTY_STRICT.enable(SerializationFeature.INDENT_OUTPUT);

        INSTANCE.setConfig(INSTANCE.getSerializationConfig().withView(Views.Forward.class));
        INSTANCE.setConfig(INSTANCE.getDeserializationConfig().withView(Views.Forward.class));

        LENIENT.setConfig(LENIENT.getSerializationConfig().withView(Views.Forward.class));
        LENIENT.setConfig(LENIENT.getDeserializationConfig().withView(Views.Forward.class));

        STRICT.setConfig(STRICT.getSerializationConfig().withView(Views.Forward.class));
        STRICT.setConfig(STRICT.getDeserializationConfig().withView(Views.Forward.class));


        PRETTY_STRICT.setConfig(PRETTY_STRICT.getSerializationConfig().withView(Views.Forward.class));
        PRETTY_STRICT.setConfig(PRETTY_STRICT.getDeserializationConfig().withView(Views.Forward.class));


        PUBLISHER.setConfig(PUBLISHER.getSerializationConfig().withView(Views.ForwardPublisher.class));
        PUBLISHER.setConfig(PUBLISHER.getDeserializationConfig().withView(Views.Forward.class));

        PRETTY_PUBLISHER.setConfig(PUBLISHER.getSerializationConfig().withView(Views.ForwardPublisher.class));
        PRETTY_PUBLISHER.setConfig(PUBLISHER.getDeserializationConfig().withView(Views.Forward.class));
        PRETTY_PUBLISHER.enable(SerializationFeature.INDENT_OUTPUT);

        BACKWARDS_PUBLISHER.setConfig(BACKWARDS_PUBLISHER.getSerializationConfig().withView(Views.Publisher.class));
        BACKWARDS_PUBLISHER.setConfig(BACKWARDS_PUBLISHER.getDeserializationConfig().withView(Views.Normal.class));
        BACKWARDS_PUBLISHER.enable(SerializationFeature.INDENT_OUTPUT);


        //PRETTY.enable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES); // This gives quite a lot of troubles. Though I'd like it to be set, especially because PRETTY is used in tests.
        PRETTY_STRICT.enable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES); // This gives quite a lot of troubles. Though I'd like it to be set, especially because PRETTY is used in tests.
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


    public static Jackson2Mapper getPrettyStrictInstance() {
        return PRETTY_STRICT;
    }


    public static Jackson2Mapper getPublisherInstance() {
        return PUBLISHER;
    }


    public static Jackson2Mapper getPrettyPublisherInstance() {
        return PRETTY_PUBLISHER;
    }

    public static Jackson2Mapper getBackwardsPublisherInstance() {
        return BACKWARDS_PUBLISHER;
    }

    public static Jackson2Mapper getThreadLocal() {
        return THREAD_LOCAL.get();
    }
    public static void setThreadLocal(Jackson2Mapper set) {
        if (set == null) {
            THREAD_LOCAL.remove();
        } else {
            THREAD_LOCAL.set(set);
        }
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

        try {
            mapper.setConfig(mapper.getDeserializationConfig().with(JsonReadFeature.ALLOW_LEADING_ZEROS_FOR_NUMBERS));
            mapper.setConfig(mapper.getDeserializationConfig().with(JsonReadFeature.ALLOW_JAVA_COMMENTS));
        } catch (NoClassDefFoundError noClassDefFoundError) {
            log.warn(noClassDefFoundError.getMessage() + " temporary falling back. Please upgrade jackson");

            mapper.enable(JsonParser.Feature.ALLOW_COMMENTS);
            mapper.enable(JsonParser.Feature.ALLOW_NUMERIC_LEADING_ZEROS);
        }

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
