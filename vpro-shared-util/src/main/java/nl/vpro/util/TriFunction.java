package nl.vpro.util;

/**
 * The next in succession of {@link java.util.function.Function} and {@link java.util.function.BiFunction}.
 *
 * A function with three arguments
 *
 * @author Michiel Meeuwissen
 * @since 1.72
 */
public interface TriFunction <T,U,V,R> {
    R apply(T t, U u, V v);
}
