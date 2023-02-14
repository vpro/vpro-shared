/*
 * Copyright (C) 2010 All rights reserved
 * VPRO The Netherlands
 */
package nl.vpro.hibernate.search6;

import java.util.Collection;
import java.util.function.Function;

import org.hibernate.search.mapper.pojo.bridge.ValueBridge;
import org.hibernate.search.mapper.pojo.bridge.runtime.ValueBridgeToIndexedValueContext;

public class CollectionSizeBridge<T> implements ValueBridge<Object, Long> {

    private final Function<T, Collection<?>> collectionFunction;
    private final String field;

    public CollectionSizeBridge(Function<T, Collection<?>> collectionFunction, String field) {
        this.collectionFunction = collectionFunction;
        this.field = field;
    }

    public CollectionSizeBridge() {
        this.collectionFunction = null;
        this.field = null;
    }

    @Override
    public Long toIndexedValue(Object value, ValueBridgeToIndexedValueContext valueBridgeToIndexedValueContext) {

        return getLong(value);
    }


    protected long getLong(Object value) {
        if (collectionFunction == null) {
            return ((Collection) value).size();
        } else {
            return collectionFunction.apply((T) value).size();
        }
    }

}
