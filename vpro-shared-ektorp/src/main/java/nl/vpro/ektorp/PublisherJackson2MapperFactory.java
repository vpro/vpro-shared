/*
 * Copyright (C) 2014 All rights reserved
 * VPRO The Netherlands
 */
package nl.vpro.ektorp;

import org.ektorp.CouchDbConnector;
import org.ektorp.impl.ObjectMapperFactory;
import com.fasterxml.jackson.databind.ObjectMapper;

import nl.vpro.jackson2.Jackson2Mapper;

/**
 * @author Michiel Meeuwissen
 * @since 5.5
 */
public class PublisherJackson2MapperFactory implements ObjectMapperFactory {

    @Override
    public ObjectMapper createObjectMapper() {
        return Jackson2Mapper.getPublisherInstance();
    }

    @Override
    public ObjectMapper createObjectMapper(CouchDbConnector connector) {
        return Jackson2Mapper.getPublisherInstance();
    }
}
