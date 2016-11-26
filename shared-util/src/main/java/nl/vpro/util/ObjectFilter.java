package nl.vpro.util;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Predicate;
import java.util.function.Supplier;

/**
 * @author Michiel Meeuwissen
 * @since 1.59
 */
public class ObjectFilter {

    public static class Result<T> {
        final T value;
        final int filtered;

        public Result(T value, int filtered) {
            this.value = value;
            this.filtered = filtered;
        }

        public T get() {
            return value;
        }
        public int filterCount() {
            return filtered;
        }
    }


    public static <T> Result<T> filter(T object, Predicate<Object> predicate) {
        AtomicInteger count = new AtomicInteger(0);
        T v = _filter(object, predicate, new HashMap<>(), count);
        return new Result<>(v, count.get());
    }

    @SuppressWarnings("unchecked")
    protected static <T> Collection<T> filterCollection(
        Collection<T> object,
        Supplier<Collection> constructor,
        Predicate<Object> predicate,
        Map<Integer, Object> objects,
        AtomicInteger filterCount
    ) {
        Collection<T> copyOfList = constructor.get();
        for (T o : object) {
            if (predicate.test(o)) {
                copyOfList.add(_filter(o, predicate, objects, filterCount));
            } else {
                filterCount.incrementAndGet();
            }
        }
        return copyOfList;

    }

    @SuppressWarnings("unchecked")
    public static <T> T _filter(final T object, final Predicate<Object> predicate, final Map<Integer, Object> objects, AtomicInteger filterCount)  {
        if (object == null) {
            return null;
        } else if (object instanceof List) {
            return (T) filterCollection((List) object, ArrayList::new, predicate, objects, filterCount);
        } else if (object instanceof Set) {
            return (T) filterCollection((Set) object, TreeSet::new, predicate, objects, filterCount);
        } else if (object instanceof CharSequence) {
            return object;
        } else if (object instanceof Number) {
            return object;
        } else if (object instanceof Enum) {
            return object;
        } else if (object instanceof Boolean) {
            return object;
        } else {
            int hash = object.hashCode();
            if (!objects.containsKey(hash)) {
                Class<T> clazz = (Class<T>) object.getClass();
                try {
                    T copy = clazz.newInstance();
                    objects.put(hash, copy);
                    for (Field f : listAllFields(clazz)) {
                        if (Modifier.isStatic(f.getModifiers())) {
                            continue;
                        }
                        if (Modifier.isTransient(f.getModifiers())) {
                            continue;
                        }
                        f.setAccessible(true);
                        Object cloned = _filter(f.get(object), predicate, objects, filterCount);
                        f.set(copy, cloned);
                    }

                    return copy;
                } catch (IllegalAccessException | InstantiationException e) {
                    throw new RuntimeException(e);
                }
            } else {
                return (T) objects.get(hash);
            }

        }
    }

    protected static List<Field> listAllFields(Class clazz) {
        List<Field> fieldList = new ArrayList<>();
        Class tmpClass = clazz;
        while (tmpClass != null) {
            fieldList.addAll(Arrays.asList(tmpClass.getDeclaredFields()));
            tmpClass = tmpClass.getSuperclass();
        }
        return fieldList;
    }

}
