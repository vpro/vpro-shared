package nl.vpro.util;


import java.util.Iterator;
import java.util.Optional;
import java.util.function.Supplier;

import org.checkerframework.checker.nullness.qual.NonNull;

/**
 * Wrap's a supplier around an iterator. This way you can delay the instantiation of the actual iterator until the first call
 * of hasNext() or next().
 * @author Michiel Meeuwissen
 */
public class LazyIterator<T> implements CloseableIterator<T>, CountedIterator<T> {

    private final Supplier<Iterator<T>> supplier;
    private Iterator<T> iterator;

    @lombok.Builder(builderClassName = "Builder")
    public LazyIterator(Supplier<Iterator<T>> supplier) {
        this.supplier = supplier;
    }

    public static <S> LazyIterator<S> of(Supplier<Iterator<S>> supplier) {
        return new LazyIterator<>(supplier);
    }

    @Override
    public boolean hasNext() {
        return getSupplied().hasNext();
    }

    @Override
    public T next() {
        return getSupplied().next();
    }

    private Iterator<T> getSupplied() {
        if (iterator == null) {
            iterator = supplier.get();
        }
        return iterator;
    }

    @Override
    public @NonNull Optional<Long> getSize() {
        getSupplied();
        if (iterator instanceof CountedIterator) {
            return ((CountedIterator) iterator).getSize();
        } else {
            return Optional.empty();
        }

    }

    @Override
    public void close() throws Exception {
        if (iterator != null && iterator instanceof AutoCloseable) {
            ((AutoCloseable) iterator).close();
        }

    }
}
