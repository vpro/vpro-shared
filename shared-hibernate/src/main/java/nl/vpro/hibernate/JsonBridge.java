/**
 * Copyright (C) 2010 All rights reserved
 * VPRO The Netherlands
 */
package nl.vpro.hibernate;

import java.io.IOException;
import java.util.Arrays;
import java.util.Map;

import org.hibernate.search.bridge.ParameterizedBridge;
import org.hibernate.search.bridge.TwoWayStringBridge;

import com.fasterxml.jackson.core.JsonProcessingException;

import nl.vpro.jackson2.Jackson2Mapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


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
    public String objectToString(Object object) {
        if (object == null) {
            return null;
        }
        try {
            String ret = Jackson2Mapper.getInstance().writeValueAsString(object);

            int len = ret.length();
            if (len > MAX_LENGTH) {
                if (object instanceof Object[]) {
                    LOG.warn("Cannot store JSON representation of object type " + object.getClass().getName() + ": trying shorter version");
                    int size = ((Object[]) object).length;
                    while (len > MAX_LENGTH && size > 0) {
                        object = Arrays.copyOfRange((Object[]) object, 0, --size);
                        ret = Jackson2Mapper.getInstance().writeValueAsString(object);
                        len = ret.length();
                    }

                    if (size == 0) {
                        LOG.warn("Cannot store JSON representation of object type " + object.getClass().getName() + ": even first item in array already too large (maxlength = " + MAX_LENGTH + ")");
                        return "{}";
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
