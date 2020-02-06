package nl.vpro.util;

import java.util.*;

import org.checkerframework.checker.nullness.qual.NonNull;

/**
 * Extension of properties that remembers insertion order.
 * @author Michiel Meeuwissen
 * @since 1.8
 */
public class OrderedProperties extends Properties {

    private final List<Object> names = new ArrayList<>();


    @Override
    public synchronized Enumeration<?> propertyNames() {
        return Collections.enumeration(names);
    }

    @NonNull
    @Override
    public Set<Map.Entry<Object, Object>> entrySet() {
        return new AbstractSet<Map.Entry<Object, Object>>() {

            @NonNull
            @Override
            public Iterator<Map.Entry<Object, Object>> iterator() {
                final Iterator<Object> i = names.iterator();
                return new Iterator<Map.Entry<Object, Object>>() {

                    @Override
                    public boolean hasNext() {
                        return i.hasNext();
                    }

                    @Override
                    public Map.Entry<Object, Object> next() {
                        Object key = i.next();
                        return new AbstractMap.SimpleEntry<>(key, OrderedProperties.this.get(key));
                    }

                    @Override
                    public void remove() {
                        throw new UnsupportedOperationException();
                    }
                };
            }

            @Override
            public int size() {
                return OrderedProperties.this.size();
            }
        };
    }

    @Override
    public synchronized Object put(Object key, Object value) {
        names.remove(key);
        names.add(key);

        return super.put(key, value);
    }

    @Override
    public synchronized Object remove(Object key) {
        names.remove(key);
        return super.remove(key);
    }
}
