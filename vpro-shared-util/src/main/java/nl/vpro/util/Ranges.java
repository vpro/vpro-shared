package nl.vpro.util;

import java.util.function.Function;

import org.checkerframework.checker.nullness.qual.Nullable;

import com.google.common.collect.Range;

public class Ranges {

    private Ranges() {

    }

    /**
     * Creates on {@link Range#openClosed(Comparable, Comparable)} range, but the arguments can be {@code null}
     * in which case unbounded ranges are created
     * @since 2.29.1
     * @see Range#closedOpen(Comparable, Comparable)
     * @throws IllegalArgumentException if {@code lower} is greater than {@code upper}
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


    /**
     * Creates on {@link Range#closed(Comparable, Comparable)} range, but the arguments can be {@code null}
     * in which case unbounded ranges are created
     * @since 2.29.3
     * @see Range#closed(Comparable, Comparable)
     * @throws IllegalArgumentException if {@code lower} is greater than {@code upper}
     */
    public static <C extends Comparable<?>> Range<C> closedClosed(@Nullable C start, @Nullable C stop) {
        if(start == null) {
            if (stop == null) {
                return Range.all();
            } else {
                return Range.atMost(stop);
            }
        } else if (stop == null) {
            return Range.atLeast(start);
        } else {
            return Range.closed(
                start,
                stop
            );
        }
    }

    public static <C extends Comparable<? super C>, D extends Comparable<? super D>> Range<D> convert(Range<C> in, Function<C, D> convertor) {
        if (in.hasLowerBound()) {
            if (in.hasUpperBound()) {
                D lower = convertor.apply(in.lowerEndpoint());
                D upper = convertor.apply(in.upperEndpoint());
                if (lower.compareTo(upper) > 0) {
                      return Range.range(
                          upper, in.upperBoundType(),
                          lower, in.lowerBoundType()
                    );
                } else {
                    return Range.range(
                        lower, in.lowerBoundType(),
                        upper, in.upperBoundType()
                    );
                }
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
