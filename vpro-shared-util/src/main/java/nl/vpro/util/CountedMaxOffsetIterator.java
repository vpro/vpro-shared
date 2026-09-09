package nl.vpro.util;

import lombok.Singular;

import java.util.List;
import java.util.function.Predicate;

import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * @author Michiel Meeuwissen
 * @since 2.23
 * @deprecated Use org.meeuw.util:mihxil-collections
 */
@Deprecated
public class CountedMaxOffsetIterator<T>  extends org.meeuw.collections.CountedMaxOffsetIterator<T> {



    @lombok.Builder(builderClassName = "Builder", builderMethodName = "_countedBuilder")
    private CountedMaxOffsetIterator(
        @NonNull CountedIterator<T> wrapped,
        @Nullable Number max,
        @Nullable Number offset,
        @Nullable @Singular List<Runnable> callbacks,
        @Nullable Predicate<T> countPredicate,
        boolean autoClose) {
        super(wrapped, max, offset, callbacks, countPredicate, autoClose);

    }
}
