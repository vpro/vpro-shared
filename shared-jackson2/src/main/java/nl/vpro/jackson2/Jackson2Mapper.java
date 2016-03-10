/*
 * Copyright (C) 2012 All rights reserved
 * VPRO The Netherlands
 */
package nl.vpro.jackson2;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.AnnotationIntrospector;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.introspect.AnnotationIntrospectorPair;
import com.fasterxml.jackson.databind.introspect.JacksonAnnotationIntrospector;
import com.fasterxml.jackson.datatype.jsr310.JSR310Module;
import com.fasterxml.jackson.module.jaxb.JaxbAnnotationIntrospector;

/**
 * @author Rico
 * @author Michiel Meeuwissen
 */

public class Jackson2Mapper extends ObjectMapper {

    private static final Logger LOG = LoggerFactory.getLogger(Jackson2Mapper.class);

    public static Jackson2Mapper INSTANCE = new Jackson2Mapper();
    public static Jackson2Mapper LENIENT = new Jackson2Mapper();
    public static Jackson2Mapper STRICT = new Jackson2Mapper();


    static {
        LENIENT.configure(DeserializationFeature.READ_UNKNOWN_ENUM_VALUES_AS_NULL, true);
        STRICT.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, true);
    }

    public static Jackson2Mapper getInstance() {
        return INSTANCE;
    }

    public static Jackson2Mapper getLenientInstance() {
        return LENIENT;
    }

    private Jackson2Mapper() {

        AnnotationIntrospector introspector = new AnnotationIntrospectorPair(
            new JacksonAnnotationIntrospector(),
            new JaxbAnnotationIntrospector(getTypeFactory()
            ));

        setSerializationInclusion(JsonInclude.Include.NON_NULL);
        setSerializationInclusion(JsonInclude.Include.NON_EMPTY);
        setAnnotationIntrospector(introspector);

        configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false); // This seems a good idea when reading from couchdb or so, but when reading user supplied forms, it is confusing not getting errors.


        configure(JsonParser.Feature.ALLOW_SINGLE_QUOTES, true);
        configure(JsonParser.Feature.ALLOW_COMMENTS, true);
        configure(JsonParser.Feature.ALLOW_UNQUOTED_FIELD_NAMES, true);
        configure(JsonParser.Feature.ALLOW_NUMERIC_LEADING_ZEROS, true);



        JSR310Module jsr310Module = new JSR310Module();
        registerModule(jsr310Module);
        registerModule(new DateModule());
        try {
            registerModule(new SerializeAvroModule());
        } catch (NoClassDefFoundError ncdfe) {
            LOG.info("SerializeAvroModule could not be registered because: " + ncdfe.getClass().getName() + " " + ncdfe.getMessage());
        }
    }
}
