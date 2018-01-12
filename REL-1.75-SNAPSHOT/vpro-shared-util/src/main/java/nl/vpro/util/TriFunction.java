package nl.vpro.util;

/**
 * @author Michiel Meeuwissen
 * @since 1.72
 */
public interface TriFunction <T,U,V,R> {
    R apply(T t, U u, V v);
}
