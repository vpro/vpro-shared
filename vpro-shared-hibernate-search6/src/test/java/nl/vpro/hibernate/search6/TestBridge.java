package nl.vpro.hibernate.search6;

import lombok.extern.log4j.Log4j2;
import org.hibernate.search.engine.backend.document.DocumentElement;
import org.hibernate.search.engine.backend.document.IndexObjectFieldReference;
import org.hibernate.search.engine.backend.types.dsl.IndexFieldTypeFactory;
import org.hibernate.search.mapper.pojo.bridge.PropertyBridge;
import org.hibernate.search.mapper.pojo.bridge.binding.PropertyBindingContext;
import org.hibernate.search.mapper.pojo.bridge.mapping.programmatic.PropertyBinder;
import org.hibernate.search.mapper.pojo.bridge.runtime.PropertyBridgeWriteContext;


import nl.vpro.hibernate.search6.domain.SubObject;

@Log4j2
public class TestBridge implements PropertyBinder {
    @Override
    public void bind(PropertyBindingContext context) {
        log.info("{}", context.bridgedElement().name());
        context.dependencies().useRootOnly();
        var field = context.indexSchemaElement().objectField(context.bridgedElement().name());
           // ADD THIS
        context.indexSchemaElement().fieldTemplate(
                "x",
            IndexFieldTypeFactory::asString
            ).multiValued();

        context.bridge(SubObject.class, new Bridge(field.toReference()));
    }


    public static class Bridge implements PropertyBridge<SubObject> {

        private final IndexObjectFieldReference field;
        public Bridge(IndexObjectFieldReference field) {
            this.field = field;
        }

        @Override
        public void write(DocumentElement target, SubObject bridgedElement, PropertyBridgeWriteContext context) {
            if (bridgedElement != null) {
                target.addValue("x",
                    bridgedElement.a
                );
            }
        }
    }
}
