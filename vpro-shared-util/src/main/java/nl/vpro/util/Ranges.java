package nl.vpro.util;

import java.util.function.Function;

import org.checkerframework.checker.nullness.qual.Nullable;

import com.google.common.collect.Range;

public class Ranges {


    /**
     * Creates on {@link Range#openClosed(Comparable, Comparable)} range, but the arguments can be {@code null}
     * in which case unbounded ranges are created
     * @since 2.29.1
     */
    public static <C extends Comparable<?>> Range<C> closedOpen(@Nullable C start, @Nullable C stop) {
        if(start == null) {
            if (stop == null) {
                return Range.all();
            } else {
                return Range.lessThan(stop);
            }
        } else if (stop == null) {
            return Range.atLeast(start);
        } else {
            return Range.closedOpen(
                start,
                stop
            );
        }
    }

    public static <C extends Comparable<?>, D extends Comparable<?>> Range<D> convert(Range<C> in, Function<C, D> convertor) {
        if (in.hasLowerBound()) {
            if (in.hasUpperBound()) {
                return Range.range(convertor.apply(in.lowerEndpoint()), in.lowerBoundType(), convertor.apply(in.upperEndpoint()), in.upperBoundType());
            } else {
                return Range.downTo(convertor.apply(in.lowerEndpoint()), in.lowerBoundType());
            }
        } else {
            if (in.hasUpperBound()) {
                return Range.upTo(convertor.apply(in.upperEndpoint()), in.upperBoundType());
            } else {
                return Range.all();
            }
        }

    }

}
