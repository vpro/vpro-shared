package nl.vpro.util;

import java.util.Objects;
import java.util.function.BiFunction;
import java.util.function.Function;

/**
 * @author Michiel Meeuwissen
 * @since 2.12
 */

public class Functions {

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



    protected static class Always<A, R> implements Function<A, R> {
        private final R val;
        private final String s;

        public Always(R val, String s) {
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
            Functions.Always<?, ?> always = (Functions.Always<?, ?>) o;
            return Objects.equals(val, always.val);
        }

        @Override
        public int hashCode() {
            return Objects.hashCode(val);
        }

        @Override
        public R apply(A a) {
            return val;
        }
    }

    protected static class BiAlways<A1, A2, R> implements BiFunction<A1, A2, R> {
        private final R val;
        private final String s;

        public BiAlways(R val, String s) {
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
            Functions.BiAlways<?, ?,  ?> always = (Functions.BiAlways<?, ?, ?>) o;
            return Objects.equals(val, always.val);
        }

        @Override
        public int hashCode() {
            return Objects.hashCode(val);
        }

        @Override
        public R apply(A1 a1, A2 a2) {
            return val;
        }
    }

     protected static class TriAlways<A1, A2, A3, R> implements TriFunction<A1, A2, A3, R> {
        private final R val;
        private final String s;

        public TriAlways(R val, String s) {
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
            Functions.TriAlways<?, ?, ?, ?> always = (Functions.TriAlways<?, ?, ?, ?>) o;
            return Objects.equals(val, always.val);
        }

        @Override
        public int hashCode() {
            return Objects.hashCode(val);
        }

        @Override
        public R apply(A1 a1, A2 a2, A3 a3) {
            return val;
        }
    }

}
