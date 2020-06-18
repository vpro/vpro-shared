package nl.vpro.util;

import java.util.Objects;
import java.util.function.BiFunction;
import java.util.function.Function;

/**
 * Provides functions implementation which always return the same value, no matter their arguments.
 *
 * With a nicer toString/equals then a standard lambda would do.
 *
 * Also a place for some other 'Function' related utilities.
 *
 * @author Michiel Meeuwissen
 * @since 2.12
 */

public final class Functions {

    private Functions() {
    }

    public static <A, R> Function<A, R> always(R v, String s) {
        return new Functions.Always<>(v, s);
    }

    public static <A, R> Function<A, R> always(R v) {
        return always(v, "always " + v);
    }

    public static <A1, A2,  R> BiFunction<A1, A2, R> biAlways(R v, String s) {
        return new Functions.BiAlways<>(v, s);
    }

    public static <A1, A2, R> BiFunction<A1, A2, R> biAlways(R v) {
        return biAlways(v, "always " + v);
    }


    public static <A1, A2, A3,  R> TriFunction<A1, A2, A3,  R> triAlways(R v, String s) {
        return new Functions.TriAlways<>(v, s);
    }

    public static <A1, A2, A3,  R> TriFunction<A1, A2, A3,  R> triAlways(R v) {
        return triAlways(v, "always " + v);
    }

    /**
     * Morphs a given {@link BiFunction} into a {@link Function}, which a certain given value for the first argument.
     *
     * See {@link TriFunction#withArg1(Object)}
     */
    public static <A1, A2, R> Function<A2, R> withArg1(BiFunction<A1, A2, R> function, A1 value) {
        return (a2) -> function.apply(value, a2);
    }

    /**
     * Morphs a given {@link BiFunction} into a {@link Function}, which a certain given value for the second argument.
     *
     * See {@link TriFunction#withArg2(Object)}
     */
    public static <A1, A2, R> Function<A1, R> withArg2(BiFunction<A1, A2, R> function, A2 value) {
        return (a1) -> function.apply(a1, value);
    }



    protected static abstract class AbstractAlways<R>  {
        protected final R val;
        private final String s;

        public AbstractAlways(R val, String s) {
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
            Functions.AbstractAlways<?> always = (Functions.AbstractAlways<?>) o;
            return Objects.equals(val, always.val);
        }

        @Override
        public int hashCode() {
            return Objects.hashCode(val);
        }
    }

    protected static final class Always<A, R> extends AbstractAlways<R> implements Function<A, R> {
        public Always(R val, String s) {
            super(val, s);
        }

        @Override
        public R apply(A a) {
            return val;
        }
    }

    protected static final class BiAlways<A1, A2, R> extends AbstractAlways<R> implements BiFunction<A1, A2, R> {
        public BiAlways(R val, String s) {
            super(val, s);
        }
        @Override
        public R apply(A1 a1, A2 a2) {
            return val;
        }
    }

     protected static final class TriAlways<A1, A2, A3, R> extends AbstractAlways<R> implements TriFunction<A1, A2, A3, R> {
        public TriAlways(R val, String s) {
            super(val, s);
        }

        @Override
        public R apply(A1 a1, A2 a2, A3 a3) {
            return val;
        }
    }

}
