package nl.vpro.util;

import java.util.Iterator;
import java.util.function.Function;

/**
 * @author Michiel Meeuwissen
 * @since 2.9
 */
public class TransformingIterator<T, W> implements CloseableIterator<T> {

    final Iterator<? extends W> wrapped;
    final Function<W, T> transformer;

    @lombok.Builder
    TransformingIterator(
        Function<W, T> transformer,
        Iterator<? extends W> wrapped) {
        this.wrapped = wrapped;
        this.transformer = transformer;
    }

    @Override
    public void close() throws Exception {
        if (wrapped instanceof AutoCloseable) {
            ((AutoCloseable) wrapped).close();
        }
    }

    T transform(W incoming) {
        return transformer.apply(incoming);
    }


    @Override
    public final boolean hasNext() {
        return this.wrapped.hasNext();
    }

    @Override
    public final T next() {
        return this.transform(this.wrapped.next());
    }

    @Override
    public final void remove() {
        this.wrapped.remove();
    }


}
