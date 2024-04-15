/*
 * Copyright (C) 2010 All rights reserved
 * VPRO The Netherlands
 */
package nl.vpro.hibernate.search6;

import org.hibernate.search.mapper.pojo.bridge.ValueBridge;
import org.hibernate.search.mapper.pojo.bridge.runtime.ValueBridgeFromIndexedValueContext;
import org.hibernate.search.mapper.pojo.bridge.runtime.ValueBridgeToIndexedValueContext;

import java.time.Instant;

public class InstantToEpochMillisBridge implements ValueBridge<Instant, Long>  {



    @Override
	public Instant fromIndexedValue(Long value, ValueBridgeFromIndexedValueContext context) {
        return Instant.ofEpochMilli(value);

    }

    @Override
    public Long toIndexedValue(Instant instance, ValueBridgeToIndexedValueContext context) {
        if (instance != null) {


            return instance.toEpochMilli();
        } else {
            return null;
        }
    }
}
