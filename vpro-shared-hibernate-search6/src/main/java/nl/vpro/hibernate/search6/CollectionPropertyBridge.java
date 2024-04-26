package nl.vpro.hibernate.search6;

import java.util.Collection;

import org.hibernate.search.engine.backend.document.DocumentElement;
import org.hibernate.search.mapper.pojo.bridge.PropertyBridge;
import org.hibernate.search.mapper.pojo.bridge.runtime.PropertyBridgeWriteContext;

/**
 * Wraps another bridge.
 * @since 5.0
 */
public class CollectionPropertyBridge<T> implements PropertyBridge<Collection> {

    private final PropertyBridge<T> single;

    public CollectionPropertyBridge(PropertyBridge<T> single) {
        this.single = single;
    }

    @Override
    public void write(DocumentElement target, Collection bridgedElements, PropertyBridgeWriteContext context) {
        for (Object bridgedElement : bridgedElements) {
            this.single.write(target, (T) bridgedElement, context);
        }
    }
}
