package nl.vpro.util;

import lombok.Lombok;
import lombok.SneakyThrows;

import java.util.concurrent.Callable;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * @author Michiel Meeuwissen
 * @since 1.78
 */
public class ExceptionUtils {

    /**
     * Wraps a {@link Callable} in a {@link Supplier}.
     *
     * You may need a supplier, but have code that throws an exception. Like this:
     *
     * {@code
     *   service.download(file, ExceptionUtils.wrapException(() -> new FileOutputStream(destFile)))
     * }
     *
     * The point is that otherwise this nice lambda gets ugly because of the try/catch block.
     */
    public static <T> Supplier<T> wrapException(Callable<T> b) {
        return () -> {
            try {
                return b.call();
            } catch (Throwable t) {
                throw Lombok.sneakyThrow(t);
            }
        };
    }


    /**
     * Wraps a {@link Callable} in a {@link Supplier}.
     *
     * You may need a supplier, but have code that throws an exception. Like this:
     *
     * {@code
     *   service.download(file, ExceptionUtils.wrapException(() -> new FileOutputStream(destFile)))
     * }
     *
     * The point is that otherwise this nice lambda gets ugly because of the try/catch block.
     */
    public static <T> Consumer<T> wrapConsumer(ThrowingConsumer<T> b) {
        return b;
    }




    @FunctionalInterface
    public interface ThrowingConsumer<T> extends Consumer<T> {

        @Override
        @SneakyThrows
        default void accept(final T e) {
            acceptWithException(e);
        }

        void acceptWithException(T e) throws Throwable;

}

}
