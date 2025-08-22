package nl.vpro.hibernate.search;

import org.apache.lucene.document.Document;
import org.hibernate.search.bridge.FieldBridge;
import org.hibernate.search.bridge.LuceneOptions;

/**
 * @author Michiel Meeuwissen
 * @since 1.10
 */
public abstract class IterableToStringBridge<T> implements FieldBridge {

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
