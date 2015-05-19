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
import com.fasterxml.jackson.databind.introspect.AnnotationIntrospectorPair;
import com.fasterxml.jackson.databind.introspect.JacksonAnnotationIntrospector;
import com.fasterxml.jackson.datatype.jsr310.JSR310Module;
import com.fasterxml.jackson.module.jaxb.JaxbAnnotationIntrospector;

/**
 * @author Rico
 * @author Michiel Meeuwissen
 */
public class Jackson2Mapper extends ObjectMapper {
    public static Jackson2Mapper INSTANCE = new Jackson2Mapper();

    public static Jackson2Mapper getInstance() {
        return INSTANCE;
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
        JSR310Module jsr310Module = new JSR310Module();
        registerModule(jsr310Module);
        registerModule(new DateModule());

    }
}
