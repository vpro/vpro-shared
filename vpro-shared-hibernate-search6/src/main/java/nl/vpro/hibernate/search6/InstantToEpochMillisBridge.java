/*
 * Copyright (C) 2010 All rights reserved
 * VPRO The Netherlands
 */
package nl.vpro.hibernate.search6;

import java.time.Instant;

import org.hibernate.search.mapper.pojo.bridge.ValueBridge;
import org.hibernate.search.mapper.pojo.bridge.runtime.ValueBridgeFromIndexedValueContext;
import org.hibernate.search.mapper.pojo.bridge.runtime.ValueBridgeToIndexedValueContext;

public class InstantToEpochMillisBridge implements ValueBridge<Instant, Long> {

    @Override
    public Instant fromIndexedValue(Long value, ValueBridgeFromIndexedValueContext context) {
        return Instant.ofEpochMilli(value);
    }

    @Override
    public Long toIndexedValue(Instant instance, ValueBridgeToIndexedValueContext context) {
        return instance == null ? null : instance.toEpochMilli();
    }


}
