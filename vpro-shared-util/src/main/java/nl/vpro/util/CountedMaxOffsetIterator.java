package nl.vpro.util;

import lombok.Singular;

import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;

import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.meeuw.functional.Unwrappable;

/**
 * @author Michiel Meeuwissen
 * @since 2.23
 */
public class CountedMaxOffsetIterator<T>  extends MaxOffsetIterator<T>  implements CountedIterator<T>, Unwrappable<CountedIterator<T>> {

    private final CountedIterator<T> wrappedCountedIterator;


    @lombok.Builder(builderClassName = "Builder", builderMethodName = "_countedBuilder")
    private CountedMaxOffsetIterator(
        @NonNull CountedIterator<T> wrapped,
        @Nullable Number max,
        @Nullable Number offset,
        @Nullable @Singular List<Runnable> callbacks,
        @Nullable Predicate<T> countPredicate,
        boolean autoClose) {
        super(wrapped, max, offset, countPredicate, true, callbacks, autoClose);
        this.wrappedCountedIterator = wrapped;
    }


    @Override
    public @NonNull Optional<Long> getSize() {
        return wrappedCountedIterator.getSize().map(i -> Math.min(i - getOffset(), max));
    }

    @Override
    public Long getCount() {
        return wrappedCountedIterator.getCount() - getOffset();
    }

    @Override
    public @NonNull Optional<Long> getTotalSize() {
        return wrappedCountedIterator.getTotalSize();
    }

    @Override
    public CountedIterator<T> unwrap() {
        return wrappedCountedIterator;
    }
}
