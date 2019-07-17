package nl.vpro.util;

import java.util.*;

import org.checkerframework.checker.nullness.qual.NonNull;

/**
 * Properties with predictable iteration order.
 * @author Michiel Meeuwissen
 * @since 0.47
 */
public class LinkedProperties extends Properties {
    private final HashSet<Object> keys = new LinkedHashSet<>();

    public LinkedProperties() {
    }

    public Iterable<Object> orderedKeys() {
        return Collections.list(keys());
    }

    @Override
    public Enumeration<Object> keys() {
        return Collections.enumeration(keys);
    }

    @NonNull
    @Override
    public Set<Map.Entry<Object, Object>> entrySet() {
        return new AbstractSet<Map.Entry<Object, Object>>() {
            @NonNull
            @Override
            public Iterator<Map.Entry<Object, Object>> iterator() {
                Iterator<Object> keyIterator = keys.iterator();
                return new Iterator<Map.Entry<Object, Object>>() {
                    @Override
                    public boolean hasNext() {
                        return keyIterator.hasNext();

                    }

                    @Override
                    public Map.Entry<Object, Object> next() {
                        Object key = keyIterator.next();
                        return new AbstractMap.SimpleEntry<>(key, get(key));
                    }
                };

            }

            @Override
            public int size() {
                return keys.size();
            }
        };
    }

    @Override
    public Object put(Object key, Object value) {
        keys.add(key);
        return super.put(key, value);
    }

}
