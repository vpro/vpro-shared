/**
 * Copyright (C) 2010 All rights reserved
 * VPRO The Netherlands
 */
package nl.vpro.hibernate;

import java.util.Collection;
import java.util.function.Function;

import org.apache.lucene.document.Document;
import org.apache.lucene.document.NumericDocValuesField;
import org.hibernate.search.bridge.FieldBridge;
import org.hibernate.search.bridge.LuceneOptions;
import org.hibernate.search.bridge.MetadataProvidingFieldBridge;
import org.hibernate.search.bridge.spi.FieldMetadataBuilder;
import org.hibernate.search.bridge.spi.FieldType;

public class CollectionSizeBridge<T> implements FieldBridge, MetadataProvidingFieldBridge {

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

    protected long getLong(Object value) {
        if (collectionFunction == null) {
            return ((Collection) value).size();
        } else {
            return collectionFunction.apply((T) value).size();
        }
    }

    @Override
    public void set(String name, Object value, Document document, LuceneOptions luceneOptions) {
        long longValue = getLong(value);
        if (field != null) {
            name = field;
        }
        luceneOptions.addNumericFieldToDocument(name, longValue, document);
        document.add(new NumericDocValuesField(name, longValue));
    }


    @Override
    public void configureFieldMetadata(String name, FieldMetadataBuilder builder) {
        builder.field(field, FieldType.LONG).sortable(true);

    }
}
