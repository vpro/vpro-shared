package nl.vpro.util;

import java.util.Objects;
import java.util.function.BiFunction;
import java.util.function.Function;

/**
 * The next in succession of {@link java.util.function.Function} and {@link java.util.function.BiFunction}.
 *
 * A function with three arguments
 *
 * @author Michiel Meeuwissen
 * @since 1.72
 */
@FunctionalInterface
public interface TriFunction <T,U,V,R> {

    R apply(T t, U u, V v);

    default BiFunction<U, V, R> withArg1(T value) {
        return (u, v) -> apply(value, u, v);
    }

    default BiFunction<T, V, R> withArg2(U value) {
        return (t, v) -> apply(t, value, v);
    }

    default BiFunction<T, U, R> withArg3(V value) {
        return (t, u) -> apply(t, u, value);
    }

    /**
     * @see java.util.function.Function#andThen(Function)
     */
    default <W> TriFunction<T, U, V, W> andThen(Function<? super R, ? extends W> after) {
        Objects.requireNonNull(after);
        return (T t, U u, V v) -> after.apply(apply(t, u, v));
    }
}
