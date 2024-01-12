package nl.vpro.util;

/**
 * @author Michiel Meeuwissen
 * @since 4.4
 */
public class ComparableUtils {

    @SafeVarargs
    public static <T extends Comparable<T>> T  max(T... values) {
        T max = null;
        for (T d : values) {
            if (d != null && (max == null || d.compareTo(max) > 0)) {
                max = d;
            }
        }
        return max;
    }

    @SafeVarargs
    public static  <T extends Comparable<T>> T min(T... values) {
        T min = null;
        for (T d : values) {
            if (d != null && (min == null || d.compareTo(min) < 0)) {
                min = d;
            }
        }
        return min;
    }

    @SafeVarargs
    public static <T>  T coalesce(T... values) {
        for (T d : values) {
            if (d != null) {
                return d;
            }
        }
        return null;
    }


}
