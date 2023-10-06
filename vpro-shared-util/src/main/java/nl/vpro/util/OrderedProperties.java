package nl.vpro.util;

import java.io.Serial;
import java.util.*;
import java.util.function.BiConsumer;

import org.checkerframework.checker.nullness.qual.NonNull;

/**
 * Extension of properties that remembers insertion order.
 * @author Michiel Meeuwissen
 * @since 1.8
 */
public class OrderedProperties extends Properties {

    @Serial
    private static final long serialVersionUID = 5640271520019127358L;

    private final List<Object> names = new ArrayList<>();


    @Override
    public synchronized Enumeration<?> propertyNames() {
        return Collections.enumeration(names);
    }

    @Override
    public synchronized @NonNull Set<Object> keySet() {
        return new LinkedHashSet<>(names);
    }

    @Override
    public synchronized @NonNull Enumeration<Object> keys() {
        return Collections.enumeration(names);
    }
    @Override
    public @NonNull Collection<Object> values() {
        return entrySet().stream().map(Map.Entry::getValue).toList();
    }


    @Override
    public synchronized void forEach(BiConsumer<? super Object, ? super Object> action) {
        entrySet().forEach(e -> action.accept(e.getKey(), e.getValue()));
    }


    @Override
    public Enumeration<Object> elements() {
        // CHM.elements() returns Iterator w/ remove() - instead wrap values()
        return Collections.enumeration(values().stream().toList());
    }

    @NonNull
    @Override
    public Set<Map.Entry<Object, Object>> entrySet() {
        return new AbstractSet<>() {

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

    @Override
    public synchronized String toString() {
        return sortedMap().toString();
    }

    protected Map<Object, Object> sortedMap() {
        return new AbstractMap<>() {
            @Override
            public @NonNull Set<Entry<Object, Object>> entrySet() {
                return OrderedProperties.this.entrySet();
            }
        };
    }

}
