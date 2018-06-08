package nl.vpro.util;

import java.util.concurrent.Callable;
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
            } catch (RuntimeException rte) {
                throw rte;
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
     };
}
}
