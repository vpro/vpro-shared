/*
 * Copyright (C) 2023 All rights reserved
 * VPRO The Netherlands
 */
package nl.vpro.hibernate.search6;

import org.hibernate.search.mapper.pojo.bridge.ValueBridge;
import org.hibernate.search.mapper.pojo.bridge.runtime.ValueBridgeToIndexedValueContext;

public class LowerCaseBridge implements ValueBridge<String, String> {

    @Override
    public String toIndexedValue(String value, ValueBridgeToIndexedValueContext context) {
        return value.toLowerCase();
    }

}
