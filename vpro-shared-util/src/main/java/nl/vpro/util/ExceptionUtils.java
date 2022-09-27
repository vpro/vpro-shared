package nl.vpro.util;

import lombok.Lombok;
import lombok.SneakyThrows;

import java.util.concurrent.Callable;
import java.util.function.*;

/**
 * @author Michiel Meeuwissen
 * @since 1.78
 */
public class ExceptionUtils {

    /**
     * Wraps a {@link Callable} in a {@link Supplier}.
     * <p>
     * You may need a supplier, but have code that throws an exception. Like this:
     * <p>
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
     * <p>
     * You may need a supplier, but have code that throws an exception. Like this:
     * <p>
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
            try {
                acceptWithException(e);
            } catch (Throwable t) {
                throw t;
            }
        }

        void acceptWithException(T e) throws Exception;

    }


    @FunctionalInterface
    public interface ThrowingFunction<A, R, E extends Exception> extends Function<A, R> {

        @Override
        @SneakyThrows
        default R apply(A a) {
            return applyWithException(a);
        }

        R applyWithException(A a) throws E;
    }

}
