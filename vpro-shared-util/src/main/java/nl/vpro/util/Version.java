package nl.vpro.util;

import java.util.Arrays;
import java.util.Optional;

import org.apache.commons.lang3.StringUtils;
import org.checkerframework.checker.nullness.qual.*;

/**
 * A version is basically a string existing of a number of parts.
 * <p>
 * This base class leaves open how these parts should look like, but we supply an extension where they are integers.
 * <p>
 * The point is that these things can now be compared in the logical way, such that e.g. 5.12.0 > 5.2.0
 *
 * @author Michiel Meeuwissen
 * @since 2.2
 */
@SuppressWarnings("unchecked")
public class Version<T extends Comparable<T>> implements Comparable<Version<T>> {
    public static final String SEPARATOR = ".";
    final T[] parts;

    public Version(T... parts) {
        this.parts = parts;
    }


    /**
     * Produces an {@link IntegerVersion}, but ignores everything after the first hyphen.
     * <p>
     * In that way something like '5.12-SNAPSHOT' will simply be equivalent to '5.12'.
     */
    @PolyNull
    public static IntegerVersion parseIntegers(@PolyNull String string) {
        if (string == null) {
            return null;
        }
        String[] parts = string.split("-", 2);
        return new IntegerVersion(parts[0]);
    }

    public static Optional<IntegerVersion> parseIntegersOptional(@Nullable String string) {
        try {
            return Optional.ofNullable(parseIntegers(string));
        } catch(Exception e) {
            return Optional.empty();
        }
    }


    public static IntegerVersion of(int... parts) {
        Integer[] integers = new Integer[parts.length];
        for (int i = 0; i < parts.length; i++) {
            integers[i] = parts[i];
        }
        return new IntegerVersion(integers);
    }

    public boolean isAfter(T... parts) {
        return isAfter(new Version<>(parts));
    }

    public boolean isAfter(Version<T> other) {
        return compareTo(other) > 0;
    }

    public boolean isNotAfter(T... parts) {
        return isNotAfter(new Version<>(parts));
    }

    public boolean isNotAfter(Version<T> other) {
        return compareTo(other) <= 0;
    }

    public boolean isBefore(T... parts) {
        return isBefore(new Version<>(parts));
    }

    public boolean isBefore(Version<T> other) {
        return compareTo(other) < 0;
    }

    public boolean isNotBefore(T... parts) {
        return isNotBefore(new Version<>(parts));
    }

    public boolean isNotBefore(Version<T> other) {
        return compareTo(other) >= 0;
    }

    @Override
    public int compareTo(@NonNull Version<T> o) {
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
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Version<?> version = (Version<?>) o;

        // Probably incorrect - comparing Object[] arrays with Arrays.equals
        return Arrays.equals(parts, version.parts);
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(parts);
    }

    @Override
    public String toString() {
        return StringUtils.join(parts, SEPARATOR);
    }
}
