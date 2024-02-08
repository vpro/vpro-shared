/*
 * Copyright (C) 2010 All rights reserved
 * VPRO The Netherlands
 */
package nl.vpro.hibernate.search6;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.search.mapper.pojo.bridge.ValueBridge;
import org.hibernate.search.mapper.pojo.bridge.runtime.ValueBridgeFromIndexedValueContext;
import org.hibernate.search.mapper.pojo.bridge.runtime.ValueBridgeToIndexedValueContext;
import org.hibernate.search.mapper.pojo.common.annotation.Param;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;


import com.fasterxml.jackson.core.JsonProcessingException;

import nl.vpro.jackson2.Jackson2Mapper;


/**
 * A straight forward bridge to store a complicated object as json in the index.
 * @since 3.5
 */
@Getter
@Slf4j
@Param(name = "class", value = "java.lang.String")
public class JsonBridge  implements ValueBridge<Object, String> {

    public final static int MAX_LENGTH = 32000;

    @Setter
    private Class<?> type;


    @Override
    public Object fromIndexedValue(String stringValue, ValueBridgeFromIndexedValueContext context) {
        if (stringValue == null) {
            return null;
        }
        try {
            return Jackson2Mapper.getLenientInstance().readValue(stringValue, type);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }


    }

    @Override
    public String toIndexedValue(Object object, ValueBridgeToIndexedValueContext valueBridgeToIndexedValueContext) {
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
                        log.warn("Cannot store JSON representation of object type " + object.getClass().getName() + ": even first item in array already too large (maxlength = " + MAX_LENGTH + ")");
                        return "[]";
                    } else {
                        log.warn("Truncated JSON representation of object type {}: {} -> {} ", object.getClass().getName(), originalSize, size);
                    }
                } else {
                    log.warn("Cannot store JSON representation of object type " + object.getClass().getName() + ": " + object + " (maxlength = " + MAX_LENGTH + ")");
                    return "{}";
                }
            }

            return ret;
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    public void setClass(String className) {
        try {
            type = Class.forName(className);
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
    }


}
