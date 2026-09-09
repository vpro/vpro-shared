package nl.vpro.util;

import lombok.ToString;

import java.util.Iterator;
import java.util.Objects;
import java.util.function.BiFunction;

/**
 * A wrapping iterator with the option to skip certain entries (based on comparing with the previous entry)
 * @author Michiel Meeuwissen
 * @since 1.68
 * @deprecated Use org.meeuw.util:mihxil-collections
 */
@ToString
@Deprecated
public class SkippingIterator<T> extends org.meeuw.collections.SkippingIterator<T> {


    @lombok.Builder(builderClassName = "Builder")
    public SkippingIterator(
        Iterator<T> wrapped,
        BiFunction<T, T, Boolean> comparator) {
        super(wrapped, comparator);
    }

    public SkippingIterator(
        Iterator<T> wrapped) {
        this(wrapped, Objects::equals);
    }

}
