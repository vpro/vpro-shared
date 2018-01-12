/*
 * Copyright (C) 2011 All rights reserved
 * VPRO The Netherlands
 */
package nl.vpro.apache.camel.format;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;

import org.apache.camel.component.jackson.JacksonDataFormat;
import org.apache.camel.converter.jaxb.JaxbDataFormat;
import org.apache.camel.spi.DataFormat;

import nl.vpro.jackson2.Jackson2Mapper;
import nl.vpro.jackson2.Views;

public abstract class AbstractFormat {

    protected static DataFormat getJaxb(String contextPath) {
        return getJaxb(contextPath, false);
    }

    protected static DataFormat getJaxb(String contextPath, boolean prettyPrint) {
        JaxbDataFormat jaxb = new JaxbDataFormat(contextPath);
        jaxb.setPrettyPrint(prettyPrint);
        return jaxb;
    }


    protected static DataFormat getJaxb(Class<?> clazz) {
        return getJaxb(clazz, false);
    }


    protected static DataFormat getJaxb(Class clazz, boolean prettyPrint) {
        try {
            JaxbDataFormat jaxb = new JaxbDataFormat(JAXBContext.newInstance(clazz));
            jaxb.setPrettyPrint(prettyPrint);
            return jaxb;
        } catch (JAXBException e) {
            throw new RuntimeException(e);
        }
    }

    protected static DataFormat getJson(Class clazz) {
        return new JacksonDataFormat(Jackson2Mapper.getInstance(), clazz, Views.Normal.class);
    }
}
