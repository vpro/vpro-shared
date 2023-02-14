package nl.vpro.hibernate.search6;

import java.util.List;

import org.apache.lucene.document.Document;
import org.hibernate.search.bridge.LuceneOptions;
import org.hibernate.search.mapper.pojo.bridge.ValueBridge;
import org.hibernate.search.mapper.pojo.bridge.runtime.ValueBridgeFromIndexedValueContext;
import org.hibernate.search.mapper.pojo.bridge.runtime.ValueBridgeToIndexedValueContext;

/**
 * @author Michiel Meeuwissen
 * @since 1.10
 */
public abstract class IterableToStringBridge<T> implements ValueBridge<Iterable<T>, List<String>> {

    @Override
    public void set(String name, Object value, Document document, LuceneOptions luceneOptions) {
        if (value != null) {
            for (T object : (Iterable<T>) value) {
                if (object != null) {
                    luceneOptions.addFieldToDocument(name, toString(object), document);
                }
            }
        }
    }


    protected abstract String toString(T object);
}
