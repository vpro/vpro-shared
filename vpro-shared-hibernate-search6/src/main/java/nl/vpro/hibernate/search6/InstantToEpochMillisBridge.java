/*
 * Copyright (C) 2010 All rights reserved
 * VPRO The Netherlands
 */
package nl.vpro.hibernate.search6;

import java.time.Instant;
import java.util.Date;
import nl.vpro.util.DateUtils;
import org.hibernate.search.mapper.pojo.bridge.ValueBridge;
import org.hibernate.search.mapper.pojo.bridge.runtime.ValueBridgeFromIndexedValueContext;
import org.hibernate.search.mapper.pojo.bridge.runtime.ValueBridgeToIndexedValueContext;

public class InstantToEpochMillisBridge implements ValueBridge<Object, Long>  {



    @Override
	public Object fromIndexedValue(Long value, ValueBridgeFromIndexedValueContext context) {
        return Instant.ofEpochMilli(value);

    }

    @Override
    public Long toIndexedValue(Object value, ValueBridgeToIndexedValueContext context) {
        if (value != null) {
            if (value instanceof Date) {
                value = DateUtils.toInstant((Date) value);
            }
            Instant instance = (Instant) value;
            return instance.toEpochMilli();
        } else {
            return null;
        }
    }
}
