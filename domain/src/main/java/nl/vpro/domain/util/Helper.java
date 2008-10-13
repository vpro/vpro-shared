/*
 * Copyright (C) 2005 All rights reserved
 * Finalist IT Group B.V. The Netherlands
 * Creation date 15-dec-2005.
 */

package nl.vpro.domain.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.sql.Blob;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Logger;

/**
 * Simple helper methods.
 *
 * @author arne, peter
 * @version $Id: Helper.java 8879 2006-09-26 14:17:19Z arne $
 */
public class Helper {

    private static final Logger log = Logger.getLogger(Helper.class);

    /**
     * Tests for equality, allowing nulls.
     *
     * @param <T> the type of o1 and o2
     * @param o1 an object
     * @param o2 another object
     * @return true when both o1 and o2 are null or when o1.equals(o2), false otherwise
     */
    public static <T> boolean equals(T o1, T o2) {
        return equals(o1, o2, true);
    }

    /**
     * Tests for equality, allowing nulls.
     *
     * @param <T> the type of o1 and o2
     * @param o1 an object
     * @param o2 another object
     * @return true when both o1 and o2 are null or when o1.equals(o2), false otherwise
     */
    public static <T> boolean equals(T o1, T o2, boolean nullMeansEqual) {
        if (nullMeansEqual) {
            return (o1 == null && o2 == null) || (o1 != null && o1.equals(o2));
        } else {
            return (o1 != null && o2 != null && o1.equals(o2));
        }
    }

    /**
     * Tests for inequality, allowing nulls.
     *
     * @param <T> the type of o1 and o2
     * @param o1 an object
     * @param o2 another object
     * @return true when either o1 or o2 is null (not both) or when !o1.equals(o2), false otherwise
     */
    public static <T> boolean notEquals(T o1, T o2) {
        return !equals(o1, o2);
    }

    /**
     * Gives a default in case the value is null.
     *
     * @param <T> the type of the value
     * @param value a value
     * @param def the default
     * @return value when value is not null, def otherwise
     * @see #nvl(Object[])
     */
    public static <T> T withDefault(T value, T def) {
        return value == null ? def : value;
    }

    /**
     * Gives a default in case the value is -1.
     *
     * @param value a value
     * @param def the default
     * @return the value if it is not -1, def otherwise
     */
    public static int withDefault(int value, int def) {
        return (value == -1) ? def : value;
    }

    /**
     * Returns the first non-null value. This method is also known as <i>coalesce</i>.
     *
     * @param <T> the type of the value
     * @param values the values
     * @return the first non-null value in the arguments, or null when values is null or when all given values in it are
     * all null
     * @see #withDefault(Object, Object)
     */
    public static <T> T nvl(T... values) {
        T result = null;
        if (values != null) {
            for (int i = 0; result == null && i < values.length; i++) {
                result = values[i];
            }
        }
        return result;
    }

    public static String nvl(String... values) {
        String result = null;
        if (values != null) {
            for (int i = 0; i < values.length; i++) {
                result = values[i];
                if (!Helper.isEmpty(result)) {
                    break;
                }
            }
        }
        return result;
    }

    /**
     * Returns the first non-null value. This method is also known as <i>coalesce</i>.
     *
     * @param values the values
     * @return the first non-null value in the arguments, or null when values is null or when all given values in it are
     * all null
     * @see #withDefault(Object, Object)
     */
    public static <T> T firstNonNull(List<T> values) {
        T result = null;
        if (!Helper.isEmpty(values)) {
            for (T v : values) {
                if (v != null) {
                    result = v;
                }
            }
        }
        return result;
    }

    /**
     * Returns the first non-null value. This method is also known as <i>coalesce</i>.
     *
     * @param values the values
     * @return the first non-null value in the arguments, or null when values is null or when all given values in it are
     * all null
     * @see #withDefault(Object, Object)
     */
    public static <T> T firstNonNull(Set<T> values) {
        T result = null;
        if (!Helper.isEmpty(values)) {
            for (T v : values) {
                if (v != null) {
                    result = v;
                }
            }
        }
        return result;
    }

    /**
     * @param str a string
     * @return true when the string is null or empty
     */
    public static boolean isEmpty(String str) {
        return str == null || str.trim().length() == 0;
    }

    /**
     * @param str a string
     * @return true when the string is not null and contains text
     */
    public static boolean isNotEmpty(String str) {
        return !isEmpty(str);
    }

    /**
     * @param str a string
     * @return true when the string is null or only contains whitespace
     */
    public static boolean isTrimmedEmpty(String str) {
        return str == null || str.trim().length() == 0;
    }

    /**
     * @param str a string
     * @return true when the string contains non-whitespace characters
     */
    public static boolean isTrimmedNotEmpty(String str) {
        return !isTrimmedEmpty(str);
    }

    /**
     * @param <T> some type
     * @param arr an array
     * @return true when arr is null or contains no elements
     */
    public static <T> boolean isEmpty(T[] arr) {
        return arr == null || arr.length == 0;
    }

    /**
     * @param <T> some type
     * @param arr an array
     * @return true when arr is not null and contains elements
     */
    public static <T> boolean isNotEmpty(T[] arr) {
        return !isEmpty(arr);
    }

    /**
     * @param <T> some type
     * @param collection the collection
     * @return true when collection is null or contains no elements
     */
    public static <T> boolean isEmpty(Collection<T> collection) {
        return collection == null || collection.isEmpty();
    }

    /**
     * @param <T> some type
     * @param collection the collection
     * @return true when collection is null or contains no elements
     */
    public static <T> boolean isEmpty(List<T> collection) {
        return collection == null || collection.isEmpty();
    }

    /**
     * @param <T> some type
     * @param collection the collection
     * @return true when collection is not null and contains elements
     */
    public static <T> boolean isNotEmpty(Collection<T> collection) {
        return !isEmpty(collection);
    }

    /**
     * Does an unchecked cast to the generic type.
     *
     * @param <T> the type to case to
     * @param o the object to cast
     * @return o
     */
    @SuppressWarnings("unchecked")
    public static <T> T uncheckedCast(Object o) {
        return (T) o;
    }

    /**
     * Adds a value to a map of lists. A value list is created when there is no entry in the map for the given key.
     *
     * @param listMap the map of lists (not null)
     * @param key the key (may be null when the map supports null keys)
     * @param value the value to put in the list
     * @param <K> key type in the map
     * @param <L> actual used key type
     * @param <V> list type in the map
     * @param <W> actual used list type
     */
    public static <K, L extends K, V, W extends V> void add(Map<K, List<V>> listMap, L key, W value) {
        List<V> list = listMap.get(key);
        if (list == null) {
            list = new ArrayList<V>();
            listMap.put(key, list);
        }
        list.add(value);
    }

    /**
     * Adds a value to a map of sets. A value set is created when there is no entry in the map for the given key.
     *
     * @param setMap the map of sets (not null)
     * @param key the key (may be null when the map supports null keys)
     * @param value the value to put in the set
     * @param <K> key type in the map
     * @param <L> actual used key type
     * @param <V> set type in the map
     * @param <W> actual used set type
     */
    public static <K, L extends K, V, W extends V> void addToSetMap(Map<K, Set<V>> setMap, L key, W value) {
        Set<V> set = setMap.get(key);
        if (set == null) {
            set = new HashSet<V>();
            setMap.put(key, set);
        }
        set.add(value);
    }

    /**
     * Puts a value in a map when the value is not null.
     *
     * @param <K> the key type
     * @param <V> the value type
     * @param map the map (not null)
     * @param key the key, can be null when the map supports it
     * @param value the value or null to do nothing
     */
    public static <K, V> void putIfNotNull(Map<K, V> map, K key, V value) {
        if (value != null) {
            map.put(key, value);
        }
    }

    /**
     * Determines whether some string is a number. No range check is performed.
     *
     * @param text the string of which must be determined if it's a number
     * @return {@code true} if {@code text} is a number, {@code false} otherwise
     */
    public static boolean isNumber(String text) {
        return (text != null) && text.matches("\\d+");
    }

    /**
     * Converts a number to another number, potentially rounding to fit the target type.
     *
     * Does all standard java types except Atomic* and Mutable*.
     *
     * @param <N> the target number type
     * @param value the number to convert
     * @param targetType the return type (not null)
     * @return value cast to type N or null when value is null. Note that the information may be discarded when the
     * value is incompatible with the target type.
     */
    public static <N extends Number> N numberCast(Number value, Class<N> targetType) {
        Number result;
        if (value == null) {
            result = null;
        } else if (targetType.equals(BigDecimal.class)) {
            if (value instanceof BigDecimal) {
                result = (BigDecimal) value;
            } else if (value instanceof BigInteger) {
                result = new BigDecimal((BigInteger) value);
            } else if (value instanceof Integer || value instanceof Long || value instanceof Short || value instanceof Byte) {

                result = BigDecimal.valueOf(((Number) value).longValue());
            } else if (value instanceof Double || value instanceof Float) {
                result = new BigDecimal(((Number) value).toString());
            } else {
                throw new IllegalArgumentException(String.format("unknown type for value %s (%s)", value, value.getClass()));
            }
        } else if (targetType.equals(BigInteger.class)) {
            if (value instanceof BigInteger) {
                result = (BigInteger) value;
            } else if (value instanceof BigDecimal) {
                result = ((BigDecimal) value).toBigInteger();
            } else {
                result = BigInteger.valueOf(((Number) value).longValue());
            }
        } else if (targetType.equals(Long.class)) {
            result = value.longValue();
        } else if (targetType.equals(Integer.class)) {
            result = value.intValue();
        } else if (targetType.equals(Short.class)) {
            result = value.shortValue();
        } else if (targetType.equals(Byte.class)) {
            result = value.byteValue();
        } else if (targetType.equals(Double.class)) {
            result = value.doubleValue();
        } else if (targetType.equals(Float.class)) {
            result = value.floatValue();
        } else {
            throw new IllegalStateException("Unkown Number type");
        }
        return Helper.<N> uncheckedCast(result);
    }

    /**
     * Tries and find an element in an array. {@link Helper#equals(Object, Object} equals} is used for comparisons. A
     * linear search is performed - if the input array is sorted, it would be wiser to use
     * {@link Arrays#binarySearch(Object[], Object)}.
     *
     * @param <T> the array type
     * @param element the element to find
     * @param array the array to search in
     * @return {@code true} if the array contains the element, {@code false} otherwise
     */
    public static <T> boolean arrayContains(T element, T... array) {
        int i = 0;
        int n = array.length;

        while ((i < n) && !equals(array[i], element)) {
            i++;
        }

        return i < n;
    }

    /**
     * Compare, taking null into account (null <).
     *
     * @param o1 first
     * @param o2 second
     * @return 1,0,-1
     */
    @SuppressWarnings("unchecked")
    public static int compare(Comparable o1, Comparable o2) {
        int result = 0;
        if (o1 != null && o2 != null) {
            result = o1.compareTo(o2);
        } else if (o1 == null) {
            result = -1;
        } else if (o2 == null) {
            result = 1;
        }
        return result;
    }

    /**
     * Convert a singleton list into the singleton element.
     *
     * @param <T> The type of the list
     * @param list the list
     * @return the singleton in the list, or <code>null</code> if the list is <code>null</code> or the size of the
     * list is zero.
     * @throws IllegalStateException when more than one element is found in the list.
     */
    public static <T> T toSingleton(List<T> list) {
        T result;
        if (list == null || list.isEmpty()) {
            result = null;
        } else if (list.size() > 1) {
            if (log.isDebugEnabled()) {
                log.debug("List " + list);
            }
            throw new IllegalStateException(String.format("The list contained %s elements, it should contain 0 or 1 elements", list.size()));
        } else {
            result = list.get(0);
        }
        return result;
    }

    /**
     * Convert a singleton set into the singleton element.
     *
     * @param <T> The type of the set
     * @param set the set
     * @return the singleton in the set, or <code>null</code> if the set is <code>null</code> or the size of the set
     * is zero.
     * @throws IllegalStateException when more than one element is found in the set.
     */
    public static <T> T toSingleton(Set<T> set) {
        T result;
        if (set == null || set.isEmpty()) {
            result = null;
        } else if (set.size() > 1) {
            if (log.isDebugEnabled()) {
                log.debug("Set " + set);
            }
            throw new IllegalStateException(String.format("The set contained %s elements, it should contain 0 or 1 elements", set.size()));
        } else {
            result = set.iterator().next();
        }
        return result;
    }

    /**
     * @param toTest The object to test
     * @param name The FQN of the class to test againts. The class should be in the classpath
     * @return true if the 'name' is assignable from 'toTest'
     */
    public static boolean isAssignableFrom(Object toTest, String name) {
        try {
            return Class.forName(name).isAssignableFrom(toTest.getClass());
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * @param toTest The FQN object to test. The class should be in the classpath
     * @param name The FQN of the class to test against. The class should be in the classpath
     * @return true if the 'name' is assignable from 'toTest'
     */
    public static boolean isAssignableFrom(String toTest, String name) {
        try {
            return Class.forName(name).isAssignableFrom(Class.forName(toTest));
        } catch (Exception e) {
            return false;
        }
    }

    @SuppressWarnings("unchecked")
    public static <T> Class<T> getClass(String name) {
        try {
            return (Class<T>) Class.forName(name);
        } catch (ClassNotFoundException e) {
            throw new IllegalArgumentException(e);
        }
    }

    @SuppressWarnings("unchecked")
    public static <W extends Annotation> String getAnnotatedProperty(Class<? extends Object> clazz, Class<W> annClazz) {
        Method[] methods = clazz.getMethods();
        String annotationProperty = null;
        for (Method method : methods) {
            if (method.getAnnotation(annClazz) != null) {
                String string1 = method.getName().substring(3, 4);
                String string2 = method.getName().substring(4);
                annotationProperty = string1.toLowerCase() + string2;
                break;
            }
        }
        if (annotationProperty == null && clazz.getSuperclass() != Object.class) {
            annotationProperty = getAnnotatedProperty((Class) clazz.getSuperclass(), annClazz);
        }
        return annotationProperty;
    }

    public static <T> List<T> removeNullEntries(List<T> source) {
        Iterator<T> iter = source.iterator();

        while (iter.hasNext()) {
            if (iter.next() == null) {
                iter.remove();
            }
        }

        return source;
    }

    public static StringBuilder appendIfNotEmpty(StringBuilder sb, String s) {

        if (!isEmpty(s)) {
            sb.append(s);
        }

        return sb;
    }

    /**
     * joins all items in the given list using String.valueOf on each item seperated by the sep String
     *
     * @param list The list to join
     * @param sep The separator String
     * @return the result
     */
    public static <T> String join(Collection<T> list, String sep) {
        if (isNotEmpty(list)) {
            StringBuilder sb = new StringBuilder();
            boolean first = true;
            for (T t : list) {
                if (!first) {
                    sb.append(sep);
                }
                sb.append(String.valueOf(t));
                first = false;
            }

            return sb.toString();
        } else {
            return "";
        }
    }

    /**
     * Determine the similar elements in both lists. Result will be sorted.
     *
     * @param a List a
     * @param b List b
     * @return The similar elements for both lists
     */
    public static <T extends Comparable<? super T>> List<T> sim(List<T> a, List<T> b) {
        Set<T> all = new HashSet<T>();
        all.addAll(a);
        all.addAll(b);

        Set<T> results = new HashSet<T>();

        for (T t : all) {
            if (a.contains(t) && b.contains(t)) {
                results.add(t);
            }
        }

        List<T> theSimilarElements = new ArrayList<T>(results);
        Collections.sort(theSimilarElements);

        return theSimilarElements;
    }

    /**
     * Will splice the number in chunks no larger then maxPartSize
     *
     * @param number The number to splice
     * @param maxPartSize The maximum size of a part in the path
     * @return A list of the spliced id
     * @author peter
     */
    public static List<String> spliceNumber(Number number, int maxPartSize) {
        return spliceNumber(number, 1, maxPartSize);
    }

    /**
     * Will splice the number in chunks no larger then maxPartSize
     *
     * @param number The number to splice
     * @param minPartSize The minimal size of a part, smaller will be skipped
     * @param maxPartSize The maximum size of a part in the path
     * @return A list of the spliced id
     * @author peter
     */
    public static List<String> spliceNumber(Number number, int minPartSize, int maxPartSize) {
        if (number != null) {
            String s = number.toString();
            int length = s.length();
            List<String> arrayList = new ArrayList<String>(length / maxPartSize + 1);
            for (int start = 0; start < length; start += maxPartSize) {
                int end = (start + maxPartSize) >= length ? length : start + maxPartSize;
                if (end - start >= minPartSize) {
                    arrayList.add(s.substring(start, end));
                }
            }

            return arrayList;
        } else {
            throw new IllegalArgumentException("number can not be null");
        }
    }

}
