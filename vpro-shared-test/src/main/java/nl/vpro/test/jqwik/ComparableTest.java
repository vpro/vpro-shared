package nl.vpro.test.jqwik;

import net.jqwik.api.*;

import nl.vpro.util.Pair;

import static java.lang.Integer.signum;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * @author Michiel Meeuwissen
 * @since 2.22
 */
public interface ComparableTest<E extends Comparable<E>> extends BasicObjectTest<E> {

    @Property
    default void  equalsConsistentWithComparable(@ForAll(EQUAL_DATAPOINTS) Pair<E, E> pair) {
        Assume.that(pair.getFirst() != null);
        Assume.that(pair.getSecond() != null);
        assertThat(pair.getFirst().compareTo(pair.getSecond())).isEqualTo(0);
    }

    @SuppressWarnings({"ResultOfMethodCallIgnored", "ConstantConditions"})
    @Property
    default void compareToNull(@ForAll(DATAPOINTS) E x) {
        Assume.that(x != null);
        assertThatThrownBy(() -> x.compareTo(null)).isInstanceOf(NullPointerException.class);
    }

    /**
     * The implementor must ensure sgn(x.compareTo(y)) == -sgn(y.compareTo(x)) for all x and y.
     */
    @Property
    default void compareToIsAntiCommutative(@ForAll(DATAPOINTS) E x, @ForAll(DATAPOINTS) E y) {
        Assume.that(x != null);
        Assume.that(y != null);
        assertThat(signum(x.compareTo(y))).isEqualTo(-1 * signum(y.compareTo(x)));

    }
    /**
     * The implementor must also ensure that the relation is transitive: (x.compareTo(y)>0 && y.compareTo(z)>0) implies x.compareTo(z)>0.
     */
    @Property(maxDiscardRatio = 1000)
    default void compareToIsTransitiveBigger(
        @ForAll(DATAPOINTS) E x,
        @ForAll(DATAPOINTS) E y,
        @ForAll(DATAPOINTS) E z) {
        Assume.that(x != null);
        Assume.that(y != null);
        Assume.that(z != null);
        Assume.that(x.compareTo(y) > 0);
        Assume.that(y.compareTo(z) > 0);

        assertThat(x.compareTo(z)).isGreaterThan(0);
    }
    @Property(maxDiscardRatio = 1000)
    default void compareToIsTransitiveSmaller(
        @ForAll(DATAPOINTS) E x,
        @ForAll(DATAPOINTS) E y,
        @ForAll(DATAPOINTS) E z) {
          Assume.that(x != null);
          Assume.that(y != null);
          Assume.that(z != null);
          Assume.that(x.compareTo(y) < 0);
          Assume.that(y.compareTo(z) < 0);

          assertThat(x.compareTo(z)).isLessThan(0);
    }

    @Property(maxDiscardRatio = 1000)
    default void compareToIsTransitiveEquals(
        @ForAll(DATAPOINTS) E x,
        @ForAll(DATAPOINTS) E y,
        @ForAll(DATAPOINTS) E z) {
        Assume.that(x != null);
        Assume.that(y != null);
        Assume.that(z != null);
        Assume.that(x.compareTo(y) == 0);
        Assume.that(y.compareTo(z) == 0);

        assertThat(x.compareTo(z)).isEqualTo(0);
    }
}
