/*
 * Copyright (C) 2010 All rights reserved
 * VPRO The Netherlands
 */
package nl.vpro.hibernate.search6;

import lombok.*;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.search.mapper.pojo.bridge.ValueBridge;
import org.hibernate.search.mapper.pojo.bridge.binding.ValueBindingContext;
import org.hibernate.search.mapper.pojo.bridge.mapping.programmatic.ValueBinder;
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
@AllArgsConstructor(access = AccessLevel.PACKAGE)
public class JsonBridge<T> implements ValueBridge<T, String> {

    public final static int MAX_LENGTH = 32000;

    private Class<T> type;

    private static final Jackson2Mapper mapper = Jackson2Mapper.getLenientInstance();


    @Override
    public T fromIndexedValue(String stringValue, ValueBridgeFromIndexedValueContext context) {
        if (stringValue == null) {
            return null;
        }
        try {
            return mapper.readValue(stringValue, type);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }


    }

    @Override
    public String parse(String stringValue) {
        return fromIndexedValue(stringValue, null).toString();
    }

    @Override
    public String toIndexedValue(T object, ValueBridgeToIndexedValueContext valueBridgeToIndexedValueContext) {
        if (object == null) {
            return null;
        }
        try {
            String ret = mapper.writeValueAsString(object);

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
                        ret = mapper.writeValueAsString(array);
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
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        JsonBridge<?> that = (JsonBridge<?>) o;
        return type.equals(that.type);
    }

    @Override
    public int hashCode() {
        return type.hashCode();
    }

    public static class Binder<T> implements ValueBinder {
        private final Class<T> type;

        public Binder(Class<T> type) {
            this.type = type;
        }


        @Override
        public void bind(ValueBindingContext<?> context) {
          /*  var t = context.typeFactory().as(type);
            t.projectable(Projectable.YES);*/

            context.bridge(type, new JsonBridge<>(type));
        }
    }


}
