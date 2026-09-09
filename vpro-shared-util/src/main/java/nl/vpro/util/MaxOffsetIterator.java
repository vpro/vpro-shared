package nl.vpro.util;

import lombok.Singular;
import lombok.extern.slf4j.Slf4j;

import java.util.Iterator;
import java.util.List;
import java.util.function.Predicate;

import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * An iterator implementing offset and max, for another iterator.
 *
 * @author Michiel Meeuwissen
 * @since 3.1
 * @deprecated Use org.meeuw.util:mihxil-collections
 */
@SuppressWarnings("UnusedReturnValue")
@Slf4j
@Deprecated
public class MaxOffsetIterator<T> extends org.meeuw.collections.MaxOffsetIterator<T> {

    public MaxOffsetIterator(Iterator<T> wrapped, Number max, boolean countNulls) {
        this(wrapped, max, 0L, countNulls);
    }

    public MaxOffsetIterator(Iterator<T> wrapped, Number max) {
        this(wrapped, max, 0L, true);
    }

    public MaxOffsetIterator(Iterator<T> wrapped, Number max, Number offset) {
        this(wrapped, max, offset, true);
    }

    public MaxOffsetIterator(Iterator<T> wrapped, Number max, Number offset, boolean countNulls) {
        this(wrapped, max, offset, null, countNulls, null, false);
    }

    @lombok.Builder(builderClassName = "Builder")
    protected MaxOffsetIterator(
        @NonNull Iterator<T> wrapped,
        @Nullable Number max,
        @Nullable Number offset,
        @Nullable Predicate<T> countPredicate,
        boolean countNulls,
        @Nullable @Singular  List<Runnable> callbacks,
        boolean autoClose) {
        super(wrapped, max, offset, countPredicate, countNulls, callbacks, autoClose);
    }


}
