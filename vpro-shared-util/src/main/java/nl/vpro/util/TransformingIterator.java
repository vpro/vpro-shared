package nl.vpro.util;

import java.util.Iterator;
import java.util.function.Function;

/**
 * @author Michiel Meeuwissen
 * @since 2.9
 * @deprecated Use org.meeuw.util:mihxil-collections
 */
@Deprecated
public class TransformingIterator<T, W> extends org.meeuw.collections.TransformingIterator<T, W> {

    @lombok.Builder
    TransformingIterator(
        Function<W, T> transformer,
        Iterator<? extends W> wrapped) {
        super(transformer, wrapped);
    }


}
