package nl.vpro.util;

import java.util.function.BiPredicate;
import java.util.function.Predicate;

/**
 * Provides alwaysFalse/alwaysTrue, with nicer toString
 * @author Michiel Meeuwissen
 * @since 2.18
 */
public final class Predicates {

    private static final String FALSE = "FALSE";
    private static final String TRUE = "TRUE";

    private Predicates() {
    }

    public static <T> Predicate<T> always(boolean v, String s) {
        return new Always<>(v, s);
    }

    public static <T> Predicate<T> alwaysFalse() {
        return always(false, FALSE);
    }
    public static <T> Predicate<T> alwaysTrue() {
        return always(true, TRUE);
    }

    public static <T, U> BiPredicate<T, U> biAlways(boolean v, String s) {
        return new BiAlways<>(v, s);
    }

    public static <T, U> BiPredicate<T, U> biAlwaysFalse() {
        return biAlways(false, FALSE);
    }
    public static <T, U> BiPredicate<T, U> biAlwaysTrue() {
        return biAlways(true, TRUE);
    }

    public static <T, U, V> TriPredicate<T, U, V> triAlways(boolean val, String s) {
        return new TriAlways<>(val, s);
    }
    public static <T, U, V> TriPredicate<T, U, V> triAlwaysFalse() {
        return triAlways(false, FALSE);
    }
    public static <T, U, V> TriPredicate<T, U, V> triAlwaysTrue() {
        return triAlways(true, TRUE);
    }

    protected static class AbstractAlways {
        protected final boolean val;
        private final String s;


        public AbstractAlways(boolean val, String s) {
            this.val = val;
            this.s = s;
        }

        @Override
        public String toString() {
            return s;
        }


        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            AbstractAlways always = (AbstractAlways) o;
            return val == always.val;
        }

        @Override
        public int hashCode() {
            return (val ? 1 : 0);
        }
    }

    protected static final class Always<T> extends AbstractAlways implements Predicate<T> {

        public Always(boolean val, String s) {
            super(val, s);
        }

        @Override
        public boolean test(T t) {
            return val;

        }
    }

    protected static final class BiAlways<T, U> extends AbstractAlways implements BiPredicate<T, U> {

        public BiAlways(boolean val, String s) {
            super(val, s);
        }
        @Override
        public boolean test(T t, U u) {
            return val;
        }
    }

    protected static final class TriAlways<T, U, V> extends AbstractAlways implements TriPredicate<T, U, V> {

        public TriAlways(boolean val, String s) {
            super(val, s);
        }

        @Override
        public boolean test(T t, U u, V v) {
            return val;
        }
    }
}
