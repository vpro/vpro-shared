/*
 * Copyright (C) 2010 All rights reserved
 * VPRO The Netherlands
 */
package nl.vpro.hibernate.search;

import java.time.Instant;
import java.util.Date;

import org.apache.lucene.document.Document;
import org.apache.lucene.document.NumericDocValuesField;
import org.hibernate.search.bridge.LuceneOptions;
import org.hibernate.search.bridge.MetadataProvidingFieldBridge;
import org.hibernate.search.bridge.builtin.NumberBridge;
import org.hibernate.search.bridge.spi.FieldMetadataBuilder;
import org.hibernate.search.bridge.spi.FieldType;

import nl.vpro.util.DateUtils;

public class InstantToMinuteBridge  extends NumberBridge implements MetadataProvidingFieldBridge {

    @Override
    public void set(String name, Object value, Document document, LuceneOptions luceneOptions) {
        if (value != null) {
            if (value instanceof Date) {
                value = DateUtils.toInstant((Date) value);
            }
            Instant instance = (Instant) value;
            document.add(new NumericDocValuesField(name, instance.toEpochMilli()));
        }


    }

    @Override
    public void configureFieldMetadata(String name, FieldMetadataBuilder builder) {
        builder.field(name, FieldType.LONG).sortable(true);

    }

    @Override
    public Object stringToObject(String stringValue) {
        return Instant.ofEpochMilli(Long.parseLong(stringValue));

    }
}
