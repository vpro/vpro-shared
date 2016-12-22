/*
 * Copyright (C) 2011 All rights reserved
 * VPRO The Netherlands
 */
package nl.vpro.apache.camel.format;

import org.apache.camel.component.jackson.JacksonDataFormat;
import org.apache.camel.converter.jaxb.JaxbDataFormat;
import org.apache.camel.spi.DataFormat;

import nl.vpro.jackson2.Jackson2Mapper;

public abstract class AbstractFormat {

    protected static DataFormat getJaxb(String contextPath) {
        return getJaxb(contextPath, false);
    }

    protected static DataFormat getJaxb(String contextPath, boolean prettyPrint) {
        JaxbDataFormat jaxb = new JaxbDataFormat(contextPath);
        jaxb.setPrettyPrint(prettyPrint);
        return jaxb;
    }

    protected static DataFormat getJson(Class clazz) {
        final JacksonDataFormat mediaMapper = new JacksonDataFormat(Jackson2Mapper.getInstance(), clazz);
        return mediaMapper;
    }
}
