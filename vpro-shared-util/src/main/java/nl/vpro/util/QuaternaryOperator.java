package nl.vpro.util;

import java.util.Comparator;
import java.util.Objects;
import java.util.function.BinaryOperator;

/**
 *  Represents an operation upon four operands of the same type, producing a result
 *  of the same type as the operands.  This is a specialization of
 *  {@link QuadriFunction} for the case where the operands and the result are all of
 *  the same type.
 *
 * @see BinaryOperator
 * @see TernaryOperator
 * @author Michiel Meeuwissen
 * @since 2.12
 */
@FunctionalInterface
public interface QuaternaryOperator<T> extends QuadriFunction<T, T, T, T, T> {

     /**
     * Returns a {@link QuaternaryOperator} which returns the lesser of four elements
     * according to the specified {@code Comparator}.
     *
     * @param <T> the type of the input arguments of the comparator
     * @param comparator a {@code Comparator} for comparing the two values
     * @return a {@code TernaryOperator} which returns the lesser of its operands,
     *         according to the supplied {@code Comparator}
     * @throws NullPointerException if the argument is null
     */
    static <T> QuaternaryOperator<T> minBy(Comparator<? super T> comparator) {
        Objects.requireNonNull(comparator);
        return (a, b, c, d) -> {
            T smaller = comparator.compare(a, b) <= 0 ? a : b;
            smaller = comparator.compare(smaller, c) <= 0 ? smaller : c;
            return comparator.compare(smaller, d) <= 0 ? smaller : d;
        };
    }

    /**
     * Returns a {@link QuaternaryOperator} which returns the greater of four elements
     * according to the specified {@code Comparator}.
     *
     * @param <T> the type of the input arguments of the comparator
     * @param comparator a {@code Comparator} for comparing the two values
     * @return a {@code TernaryOperator} which returns the greater of its operands,
     *         according to the supplied {@code Comparator}
     * @throws NullPointerException if the argument is null
     */
    static <T> QuaternaryOperator<T> maxBy(Comparator<? super T> comparator) {
        Objects.requireNonNull(comparator);
        return (a, b, c, d) -> {
            T bigger = comparator.compare(a, b) >= 0 ? a : b;
            bigger = comparator.compare(bigger, c) >= 0 ? bigger : c;
            return comparator.compare(bigger, d) >= 0 ? bigger : d;
        };
    }
}
