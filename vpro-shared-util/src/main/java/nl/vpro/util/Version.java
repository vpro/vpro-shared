package nl.vpro.util;

import org.apache.commons.lang3.StringUtils;

/**
 * @author Michiel Meeuwissen
 * @since 2.2
 */
@SuppressWarnings("unchecked")
public class Version<T extends Comparable<T>> implements Comparable<Version<T>> {
    final T[] parts;

    public Version(T... parts) {
        this.parts = parts;
    }

    public static IntegerVersion parseIntegers(String string) {
        return new IntegerVersion(string);
    }


    public static IntegerVersion of(int... parts) {
        Integer[] integers = new Integer[parts.length];
        for (int i = 0; i < parts.length; i++) {
            integers[i] = parts[i];
        }
        return new IntegerVersion(integers);
    }

    public boolean isAfter(T... parts) {
        return isAfter(new Version<T>(parts));
    }

    public boolean isAfter(Version<T> other) {
        return compareTo(other) > 0;
    }
    public boolean isBefore(T... parts) {
        return isBefore(new Version<T>(parts));
    }

    public boolean isBefore(Version<T> other) {
        return compareTo(other) < 0;
    }

    public boolean isNotBefore(T... parts) {
        return isNotBefore(new Version<T>(parts));
    }

    public boolean isNotBefore(Version<T> other) {
        return compareTo(other) >= 0;
    }

    @Override
    public int compareTo(Version<T> o) {
        int i = 0;
        while(true) {
            if (parts.length == i || o.parts.length == i) {
                return parts.length - o.parts.length;

            }
            int compare = parts[i].compareTo(o.parts[i]);
            if (compare != 0) {
                return compare;
            }
            i++;
        }

    }
    @Override
    public String toString() {
        return StringUtils.join(parts, ".");
    }
}
