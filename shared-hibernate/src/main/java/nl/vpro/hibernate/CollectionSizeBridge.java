/**
 * Copyright (C) 2010 All rights reserved
 * VPRO The Netherlands
 */
package nl.vpro.hibernate;

import java.util.Collection;

import org.hibernate.search.bridge.StringBridge;

public class CollectionSizeBridge implements StringBridge {

    @Override
    public String objectToString(Object object) {
        if(object == null) {
            return "000";
        }

        if(!(object instanceof Collection)) {
            throw new IllegalArgumentException("Must provide a collection.");
        }
        int size = ((Collection)object).size();
        return String.format("%03d",  size);
    }
}
