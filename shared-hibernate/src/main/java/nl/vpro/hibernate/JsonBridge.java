/**
 * Copyright (C) 2010 All rights reserved
 * VPRO The Netherlands
 */
package nl.vpro.hibernate;

import java.io.IOException;
import java.util.Map;

import org.hibernate.search.bridge.ParameterizedBridge;
import org.hibernate.search.bridge.TwoWayStringBridge;

import com.fasterxml.jackson.core.JsonProcessingException;

import nl.vpro.jackson2.Jackson2Mapper;


/**
 * A straight forward bridge to store a complicated object as json in the index.
 * @since 3.5
 */
public class JsonBridge implements TwoWayStringBridge, ParameterizedBridge {

    private Class clazz;

    @Override
    public Object stringToObject(String stringValue) {
        if (stringValue == null) {
            return null;
        }
        try {
            return Jackson2Mapper.getInstance().readValue(stringValue, clazz);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }


    }

    @Override
    public String objectToString(Object object) {
        if (object == null) {
            return null;
        }
        try {
            return Jackson2Mapper.getInstance().writeValueAsString(object);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void setParameterValues(Map<String, String> parameters) {
        try {
            String className = parameters.get("class");
            clazz = Class.forName(className);
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
    }
}
