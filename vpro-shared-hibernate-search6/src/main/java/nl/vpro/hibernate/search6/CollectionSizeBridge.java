/*
 * Copyright (C) 2010 All rights reserved
 * VPRO The Netherlands
 */
package nl.vpro.hibernate.search6;

import lombok.With;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.search.engine.backend.document.DocumentElement;
import org.hibernate.search.engine.backend.types.*;
import org.hibernate.search.mapper.pojo.bridge.PropertyBridge;
import org.hibernate.search.mapper.pojo.bridge.binding.PropertyBindingContext;
import org.hibernate.search.mapper.pojo.bridge.mapping.programmatic.PropertyBinder;
import org.hibernate.search.mapper.pojo.bridge.runtime.PropertyBridgeWriteContext;

import java.util.Collection;
import java.util.function.Function;
import java.util.function.Predicate;

@Slf4j
public class CollectionSizeBridge implements PropertyBridge<Collection> {

    private final String field;

    private final Function<Collection, Integer> sizeFunction;

    private  CollectionSizeBridge(String field, Function<Collection, Integer> sizeFunction) {
        this.field = field;
        this.sizeFunction = sizeFunction;
    }

    @Override
    public void write(DocumentElement target, Collection bridgedElement, PropertyBridgeWriteContext context) {
        if (bridgedElement != null) {
            target.addValue(field, this.sizeFunction.apply(bridgedElement));
        } else {
            target.addValue(field, 0);
        }
    }


    public static class Binder<E> implements PropertyBinder {

        @With
        private final String name;

        @With
        private final Predicate<E> collectionFilter;


        private Binder(String name, Predicate<E> collectionFilter) {
            this.name = name;
            this.collectionFilter = collectionFilter;
        }


        public Binder() {
            this.collectionFilter  = null;
            this.name = null;
        }


        @Override
        public void bind(PropertyBindingContext context) {
            context.dependencies().useRootOnly();
            var name = this.name == null ? context.bridgedElement().name() + "Size" : this.name;
            var type = context.typeFactory().asInteger().sortable(Sortable.YES).projectable(Projectable.YES).searchable(Searchable.YES);
            var field = context.indexSchemaElement().field(name , type);
            log.debug("Defining field {} with type {}", name, type);
            field.toReference();
            Function<Collection, Integer> size = collectionFilter == null ? Collection::size : c -> (int) (c.stream().filter(collectionFilter).count());
            context.bridge(Collection.class, new CollectionSizeBridge( name, size));
        }
    }

}
