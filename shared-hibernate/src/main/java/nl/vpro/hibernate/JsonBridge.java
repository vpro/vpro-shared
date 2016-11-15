/**
 * Copyright (C) 2010 All rights reserved
 * VPRO The Netherlands
 */
package nl.vpro.hibernate;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Map;

import org.hibernate.search.bridge.ParameterizedBridge;
import org.hibernate.search.bridge.TwoWayStringBridge;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonProcessingException;

import nl.vpro.jackson2.Jackson2Mapper;


/**
 * A straight forward bridge to store a complicated object as json in the index.
 * @since 3.5
 */
public class JsonBridge implements TwoWayStringBridge, ParameterizedBridge {

    private final static Logger LOG = LoggerFactory.getLogger(JsonBridge.class);

    public final static int MAX_LENGTH = 32000;

    private Class clazz;


    @Override
    public Object stringToObject(String stringValue) {
        if (stringValue == null) {
            return null;
        }
        try {
            return Jackson2Mapper.getLenientInstance().readValue(stringValue, clazz);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }


    }

    @Override
    public String objectToString(final Object object) {
        if (object == null) {
            return null;
        }
        try {
            String ret = Jackson2Mapper.getInstance().writeValueAsString(object);

            int len = ret.length();
            if (len > MAX_LENGTH) {
                if (object instanceof Collection || object instanceof Object[]) {

                    Object[] array;
                    if (object instanceof Object[]) {
                        array = (Object[]) object;
                    } else {
                        array = ((Collection) object).stream().toArray(i -> new Object[((Collection) object).size()]);
                    }
                    int originalSize = array.length;
                    int size = array.length;
                    while (len > MAX_LENGTH && array.length > 0) {
                        array = Arrays.copyOfRange(array, 0, --size);
                        ret = Jackson2Mapper.getInstance().writeValueAsString(array);
                        len = ret.length();
                    }
                    if (size == 0) {
                        LOG.warn("Cannot store JSON representation of object type " + object.getClass().getName() + ": even first item in array already too large (maxlength = " + MAX_LENGTH + ")");
                        return "[]";
                    } else {
                        LOG.warn("Truncated JSON representation of object type {}: {} -> {} ", object.getClass().getName(), originalSize, size);
                    }
                } else {
                    LOG.warn("Cannot store JSON representation of object type " + object.getClass().getName() + ": " + object + " (maxlength = " + MAX_LENGTH + ")");
                    return "{}";
                }
            }

            return ret;
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
