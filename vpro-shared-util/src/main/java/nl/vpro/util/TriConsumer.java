package nl.vpro.util;

import java.io.IOException;

/**
 * The next in succession of {@link java.util.function.Consumer} and {@link java.util.function.BiConsumer}.
 *
 * A function with three arguments
 *
 * @author Michiel Meeuwissen
 * @since 2.7.0
 */
public interface TriConsumer<T,U,V> {
    void accept(T t, U u, V v) throws IOException;
}
