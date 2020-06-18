package nl.vpro.util;

import java.util.Objects;
import java.util.function.BiFunction;
import java.util.function.Function;

/**
 * The next in succession of {@link Function} and {@link BiFunction}.
 *
 * A function with four arguments
 *
 * @author Michiel Meeuwissen
 * @since 2.12
 */
@FunctionalInterface
public interface QuadriFunction<T,U,V,W,R> {

    R apply(T t, U u, V v, W w);

    /**
     * Morphs this {@link QuadriFunction} into a {@link TriFunction}, which a certain given value for the first argument.
     *
     */
    default TriFunction<U, V, W, R> withArg1(T value) {
        return (u, v, w) -> apply(value, u, v ,w);
    }

    /**
     * Morphs this {@link QuadriFunction} into a {@link TriFunction}, which a certain given value for the second argument.
     *
     * See {@link Functions#withArg2(BiFunction, Object)}
     */
    default TriFunction<T, V, W, R> withArg2(U value) {
        return (t, v, w) -> apply(t, value, v, w);
    }

    /**
     * Morphs this {@link QuadriFunction} into a {@link TriFunction}, which a certain given value for the third argument.
     */
    default TriFunction<T, U, W, R> withArg3(V value) {
        return (t, u, w) -> apply(t, u, value, w);
    }

     /**
     * Morphs this {@link QuadriFunction} into a {@link TriFunction}, which a certain given value for the fourth argument.
     */
    default TriFunction<T, U, V, R> withArg4(W value) {
        return (t, u, v) -> apply(t, u, v, value);
    }

    /**
     * @see Function#andThen(Function)
     */
    default <S> QuadriFunction<T, U, V, W, S> andThen(Function<? super R, ? extends S> after) {
        Objects.requireNonNull(after);
        return (T t, U u, V v, W w) -> after.apply(apply(t, u, v, w));
    }
}
