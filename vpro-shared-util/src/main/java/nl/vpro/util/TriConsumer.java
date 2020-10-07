package nl.vpro.util;

import java.util.Objects;
import java.util.function.Consumer;

/**
 * The next in succession of {@link java.util.function.Consumer} and {@link java.util.function.BiConsumer}.
 *
 * A function with three arguments
 *
 * @author Michiel Meeuwissen
 * @since 2.7.0
 */
@FunctionalInterface
public interface TriConsumer<T, U, V> {

    void accept(T t, U u, V v);

    /**
     * @see Consumer#andThen(java.util.function.Consumer)
     */
    default TriConsumer<T, U, V> andThen(TriConsumer<? super T, ? super U, ? super V> after) {
        Objects.requireNonNull(after);
        return (t, u, v ) -> {
            accept(t, u, v);
            after.accept(t, u, v);
        };
    }
}
