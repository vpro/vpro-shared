package nl.vpro.hibernate;

import org.apache.lucene.document.Document;
import org.hibernate.search.bridge.FieldBridge;
import org.hibernate.search.bridge.LuceneOptions;

/**
 * @author Michiel Meeuwissen
 * @since 1.10
 */
public class IterableToStringBridge implements FieldBridge {

    @Override
    public void set(String name, Object value, Document document, LuceneOptions luceneOptions) {
        if (value != null) {
            for (Object object : (Iterable<?>) value) {
                if (object != null) {
                    luceneOptions.addFieldToDocument(name, object.toString(), document);
                }
            }
        }
    }
}
