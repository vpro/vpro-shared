package nl.vpro.util;

import lombok.EqualsAndHashCode;

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

    /**
     * Creates a new {@link TriPredicate} but implement it using a {@link BiPredicate}, simply completely ignoring the third argument
     */
    public static <T, U, V> TriPredicate<T, U, V> ignoreArg3(BiPredicate<T, U> biPredicate) {
        return new BiWrapper<T, U, V>(biPredicate, "ignore arg3") {
            @Override
            public boolean test(T t, U u, V v) {
                return biPredicate.test(t, u);

            }
        };
    }
    /**
     * Creates a new {@link TriPredicate} but implement it using a {@link BiPredicate}, simply completely ignoring the second argument
     */
    public static <T, U, V> TriPredicate<T, U, V> ignoreArg2(BiPredicate<T, V> biPredicate) {
        return new BiWrapper<T, U, V>(biPredicate, "ignore arg2") {
            @Override
            public boolean test(T t, U u, V v) {
                return biPredicate.test(t, v);

            }
        };
    }
    /**
     * Creates a new {@link TriPredicate} but implement it using a {@link BiPredicate}, simply completely ignoring the first argument
     */
    public static <T, U, V> TriPredicate<T, U, V> ignoreArg1(BiPredicate<U, V> biPredicate) {
        return new BiWrapper<T, U, V>(biPredicate, "ignore arg1") {
            @Override
            public boolean test(T t, U u, V v) {
                return biPredicate.test(u, v);

            }
        };
    }

    /**
     * Creates a new {@link BiPredicate} but implement it using a {@link Predicate}, simply completely ignoring the second argument
     */
    public static <T, U> BiPredicate<T, U> ignoreArg2(Predicate<T> predicate) {
        return new Wrapper<T, U>(predicate, "ignore arg2") {
            @Override
            public boolean test(T t, U u) {
                return predicate.test(t);

            }
        };
    }
    /**
     * Creates a new {@link BiPredicate} but implement it using a {@link Predicate}, simply completely ignoring the first argument
     */
    public static <T, U> BiPredicate<T, U> ignoreArg1(Predicate<U> predicate) {
        return new Wrapper<T, U>(predicate, "ignore arg1") {
            @Override
            public boolean test(T t, U u) {
                return predicate.test(u);

            }
        };
    }


    protected static abstract class AbstractAlways {
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

    @EqualsAndHashCode
    protected static abstract  class BiWrapper<X, Y, Z> implements TriPredicate<X, Y, Z> {

        private final BiPredicate<?, ?> wrapped;
        @EqualsAndHashCode.Exclude
        private final String why;

        public BiWrapper(BiPredicate<?, ?> wrapped, String why) {
            this.wrapped = wrapped;
            this.why = why;
        }

        @Override
        public String toString() {
            return wrapped.toString() + "(" + why  + ")";
        }
    }
    @EqualsAndHashCode
    protected static abstract  class Wrapper<X, Y> implements BiPredicate<X, Y> {

        private final Predicate<?> wrapped;
        @EqualsAndHashCode.Exclude
        private final String why;

        public Wrapper(Predicate<?> wrapped, String why) {
            this.wrapped = wrapped;
            this.why = why;
        }

        @Override
        public String toString() {
            return wrapped.toString() + "(" + why  + ")";
        }


    }
}
