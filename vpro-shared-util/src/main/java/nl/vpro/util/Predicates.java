package nl.vpro.util;

import java.util.function.BiPredicate;
import java.util.function.Predicate;

/**
 * Provides alwaysFalse/alwaysTrue, with nicer toString
 * @author Michiel Meeuwissen
 * @since 2.18
 */
public class Predicates {

     public static <T> Predicate<T> always(boolean v, String s) {
        return new Predicate<T>() {
            @Override
            public boolean test(T t) {
                return v;
            }
            @Override
            public String toString() {
                return s;
            }
        };
    }

    public static <T> Predicate<T> alwaysFalse() {
        return always(false, "FALSE");
    }
    public static <T> Predicate<T> alwaysTrue() {
        return always(true, "TRUE");
    }

    public static <T, U> BiPredicate<T, U> biAlways(boolean v, String s) {
        return new BiPredicate<T, U>() {
            @Override
            public boolean test(T t, U u) {
                return v;

            }
            @Override
            public String toString() {
                return s;
            }
        };
    }

    public static <T, U> BiPredicate<T, U> biAlwaysFalse() {
        return biAlways(false, "FALSE");
    }
    public static <T, U> BiPredicate<T, U> biAlwaysTrue() {
        return biAlways(true, "TRUE");
    }

    public static <T, U, V> TriPredicate<T, U, V> triAlways(boolean val, String s) {
        return new TriPredicate<T, U, V>() {
            @Override
            public boolean test(T t, U u, V v) {
                return val;

            }
            @Override
            public String toString() {
                return s;
            }
        };
    }
    public static <T, U, V> TriPredicate<T, U, V> triAlwaysFalse() {
        return triAlways(false, "FALSE");
    }
    public static <T, U, V> TriPredicate<T, U, V> triAlwaysTrue() {
        return triAlways(true, "TRUE");
    }
}
