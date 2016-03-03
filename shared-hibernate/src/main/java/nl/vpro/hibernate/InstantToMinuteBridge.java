/**
 * Copyright (C) 2010 All rights reserved
 * VPRO The Netherlands
 */
package nl.vpro.hibernate;

import org.apache.lucene.document.Document;
import org.hibernate.search.bridge.FieldBridge;
import org.hibernate.search.bridge.LuceneOptions;

public class InstantToMinuteBridge  implements FieldBridge {

    @Override
    public void set(String name, Object value, Document document, LuceneOptions luceneOptions) {
        if (value == null) {
            return;
        }


    }
}
