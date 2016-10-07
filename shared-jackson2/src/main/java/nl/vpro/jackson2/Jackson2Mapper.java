/*
 * Copyright (C) 2012 All rights reserved
 * VPRO The Netherlands
 */
package nl.vpro.jackson2;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.AnnotationIntrospector;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.introspect.AnnotationIntrospectorPair;
import com.fasterxml.jackson.databind.introspect.JacksonAnnotationIntrospector;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.module.jaxb.JaxbAnnotationIntrospector;
import lombok.extern.slf4j.Slf4j;

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

    static {
        LENIENT.enable(DeserializationFeature.READ_UNKNOWN_ENUM_VALUES_AS_NULL);
        STRICT.enable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
        PRETTY.enable(SerializationFeature.INDENT_OUTPUT);
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

    private Jackson2Mapper() {

        AnnotationIntrospector introspector = new AnnotationIntrospectorPair(
            new JacksonAnnotationIntrospector(),
            new JaxbAnnotationIntrospector(getTypeFactory()
            ));

        setSerializationInclusion(JsonInclude.Include.NON_NULL);
        setSerializationInclusion(JsonInclude.Include.NON_EMPTY);
        setAnnotationIntrospector(introspector);

        disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES); // This seems a good idea when reading from couchdb or so, but when reading user supplied forms, it is confusing not getting errors.

        enable(JsonParser.Feature.ALLOW_SINGLE_QUOTES);
        enable(JsonParser.Feature.ALLOW_COMMENTS);
        enable(JsonParser.Feature.ALLOW_UNQUOTED_FIELD_NAMES);
        enable(JsonParser.Feature.ALLOW_NUMERIC_LEADING_ZEROS);

        JavaTimeModule javaTimeModule = new JavaTimeModule();
        registerModule(javaTimeModule);
        registerModule(new DateModule());
        try {
            registerModule(new SerializeAvroModule());
        } catch (NoClassDefFoundError ncdfe) {
            if (! loggedAboutAvro) {
                log.info("SerializeAvroModule could not be registered because: " + ncdfe.getClass().getName() + " " + ncdfe.getMessage());
            }
            loggedAboutAvro = true;
        }
    }
}
