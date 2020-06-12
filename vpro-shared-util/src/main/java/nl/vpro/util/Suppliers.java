package nl.vpro.util;

import java.util.function.Supplier;

/**
 * @author Michiel Meeuwissen
 * @since 2.12
 */
public class Suppliers {

    private Suppliers() {

    }

    static <T> Supplier<T> memoize(Supplier<T> supplier) {
        return new MemoizeSupplier<>(supplier);
    }


    protected static class MemoizeSupplier<T> implements Supplier<T> {

        Supplier<T> wrapped;

        T value;

        public MemoizeSupplier(Supplier<T> supplier) {
            wrapped = supplier;
        }

        @Override
        public T get() {
            if (wrapped != null) {
                synchronized (this) {
                    if (wrapped != null) {
                        value = wrapped.get();
                        wrapped = null;
                    }
                }
            }
            return value;

        }
    }
}
