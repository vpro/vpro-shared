/**
 * Copyright (C) 2010 All rights reserved
 * VPRO The Netherlands
 */
package nl.vpro.hibernate;

import java.util.Collection;

import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.StringField;
import org.hibernate.search.bridge.LuceneOptions;
import org.hibernate.search.bridge.StringBridge;
import org.hibernate.search.bridge.TwoWayFieldBridge;

public class CollectionSizeBridge implements StringBridge, TwoWayFieldBridge {


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


    @Override
    public Object get(String name, Document document) {
        return Integer.parseInt(document.get(name));

    }
    @Override
    public void set(String name, Object value, Document document, LuceneOptions luceneOptions) {
        Field field = new StringField(name, objectToString(value), luceneOptions.getStore());
        document.add(field);
    }
}
