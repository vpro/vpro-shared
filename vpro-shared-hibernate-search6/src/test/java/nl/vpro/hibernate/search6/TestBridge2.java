package nl.vpro.hibernate.search6;

import lombok.extern.log4j.Log4j2;
import org.hibernate.search.engine.backend.document.DocumentElement;
import org.hibernate.search.engine.backend.document.IndexObjectFieldReference;
import org.hibernate.search.mapper.pojo.bridge.PropertyBridge;
import org.hibernate.search.mapper.pojo.bridge.binding.PropertyBindingContext;
import org.hibernate.search.mapper.pojo.bridge.mapping.programmatic.PropertyBinder;
import org.hibernate.search.mapper.pojo.bridge.runtime.PropertyBridgeWriteContext;

@Log4j2
public class TestBridge2 implements PropertyBinder {
    @Override
    public void bind(PropertyBindingContext context) {
        log.info("{}", context.bridgedElement().name());
        context.dependencies().useRootOnly();
        ///context.indexSchemaElement().field("a").asString().createAccessor();
        var text = context.indexSchemaElement().objectField(context.bridgedElement().name());

        context.bridge(String.class, new Bridge(text.toReference()));
    }


    public static class Bridge implements PropertyBridge<String> {


        private final IndexObjectFieldReference reference;
        public Bridge(IndexObjectFieldReference reference) {
            this.reference = reference;
        }

        @Override
        public void write(DocumentElement target, String bridgedElement, PropertyBridgeWriteContext context) {


            target.addValue("x",
                bridgedElement.substring(1)
            );
        }
    }
}
