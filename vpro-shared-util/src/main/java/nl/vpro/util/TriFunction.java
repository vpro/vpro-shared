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
 * @deprecated Moved to separate project
 */
@FunctionalInterface
@Deprecated
public interface TriFunction <T,U,V,R> {

    R apply(T t, U u, V v);

    /**
     * Morphs this {@link TriFunction} into a {@link BiFunction}, which a certain given value for the first argument.
     *
     * See {@link Functions#withArg1(BiFunction, Object)}
     */
    default BiFunction<U, V, R> withArg1(T value) {
        return (u, v) -> apply(value, u, v);
    }

    /**
     * Morphs this {@link TriFunction} into a {@link BiFunction}, which a certain given value for the second argument.
     *
     * See {@link Functions#withArg2(BiFunction, Object)}
     */
    default BiFunction<T, V, R> withArg2(U value) {
        return (t, v) -> apply(t, value, v);
    }

    /**
     * Morphs this {@link TriFunction} into a {@link BiFunction}, which a certain given value for the third argument.
     */
    default BiFunction<T, U, R> withArg3(V value) {
        return (t, u) -> apply(t, u, value);
    }

    /**
     * @see java.util.function.Function#andThen(Function)
     */
    default <S> TriFunction<T, U, V, S> andThen(Function<? super R, ? extends S> after) {
        Objects.requireNonNull(after);
        return (T t, U u, V v) -> after.apply(apply(t, u, v));
    }
}
