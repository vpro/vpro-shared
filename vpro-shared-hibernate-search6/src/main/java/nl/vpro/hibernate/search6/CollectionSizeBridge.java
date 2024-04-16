/*
 * Copyright (C) 2010 All rights reserved
 * VPRO The Netherlands
 */
package nl.vpro.hibernate.search6;

import lombok.extern.slf4j.Slf4j;

import java.util.Collection;

import org.hibernate.search.engine.backend.document.DocumentElement;
import org.hibernate.search.engine.backend.document.IndexFieldReference;
import org.hibernate.search.engine.backend.types.*;
import org.hibernate.search.mapper.pojo.bridge.PropertyBridge;
import org.hibernate.search.mapper.pojo.bridge.binding.PropertyBindingContext;
import org.hibernate.search.mapper.pojo.bridge.mapping.programmatic.PropertyBinder;
import org.hibernate.search.mapper.pojo.bridge.runtime.PropertyBridgeWriteContext;

@SuppressWarnings("rawtypes")
@Slf4j
public class CollectionSizeBridge implements PropertyBridge<Collection> {

    private final String field;
    private final IndexFieldReference<Integer> indexSchemaObjectField;

    public CollectionSizeBridge(String field, IndexFieldReference<Integer> indexSchemaObjectField) {
        this.field = field;
        this.indexSchemaObjectField = indexSchemaObjectField;
    }

    @Override
    public void write(DocumentElement target, Collection bridgedElement, PropertyBridgeWriteContext context) {
        if (bridgedElement != null) {
            target.addValue(field, bridgedElement.size());
        } else {
            target.addValue(field, 0);
        }
    }


    public static class Binder implements PropertyBinder {

        @Override
        public void bind(PropertyBindingContext context) {
            context.dependencies().useRootOnly();
            var name = context.bridgedElement().name() + "Size";
            var type = context.typeFactory().asInteger().sortable(Sortable.YES).projectable(Projectable.YES).searchable(Searchable.YES);
            var field = context.indexSchemaElement().field(name , type);
            log.info("Defining field {} with type {}", name, type);
            context.bridge(Collection.class, new CollectionSizeBridge(name, field.toReference()));
        }
    }

}
