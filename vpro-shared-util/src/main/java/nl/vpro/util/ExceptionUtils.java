package nl.vpro.util;

import lombok.Lombok;

import java.util.concurrent.Callable;
import java.util.function.*;

import org.meeuw.functional.ThrowAnyConsumer;

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
                throw sneakyThrow(t);
            }
        };
    }

    /**
     * Wraps a {@link ThrowingFunction} in a {@link Function}.
     * The point is that otherwise this nice lambda gets ugly because of the try/catch block.
     */
    public static <A, R, E extends Exception> Function<A, R> wrapException(org.meeuw.functional.ThrowingFunction<A, R, E> f) {
        return (a) -> {
            try {
                return f.apply(a);
            } catch (Throwable t) {
                throw sneakyThrow(t);
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
    public static <T> Consumer<T> wrapConsumer(org.meeuw.functional.ThrowingConsumer<T, ?> b) {
        return b;
    }




    @FunctionalInterface
    @Deprecated
    public interface ThrowingConsumer<T> extends ThrowAnyConsumer<T> {

    }


    @FunctionalInterface
    @Deprecated
    public interface ThrowingFunction<A, R, E extends Exception> extends org.meeuw.functional.ThrowingFunction<A, R, E> {


    }

    /**
     * Exactly like {@link Lombok#sneakyThrow(Throwable)}, but without the lombok dependency
     *
     */
    public static RuntimeException sneakyThrow(Throwable t) {

        if (t == null) throw new NullPointerException("t");
        return sneakyThrow0(t);
    }

    @SuppressWarnings("unchecked")
    private static <T extends Throwable> T sneakyThrow0(Throwable t) throws T {
        throw (T)t;
    }

}
