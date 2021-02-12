package nl.vpro.test.jwik;


import net.jqwik.api.*;

import static org.assertj.core.api.Assertions.assertThat;


/**
 * @author Michiel Meeuwissen
 * @since ...
 */
public interface BasicObjectTest<E> {

    String ELEMENTS = "elements";

    /**
     * For any non-null reference value x, x.equals(x) should return true
     */
    @Property
    default void equalsIsReflexive(@ForAll(ELEMENTS) E x) {
        Assume.that(x != null);;
        assertThat(x.equals(x)).isTrue();
    }

    /**
     * For any non-null reference values x and y, x.equals(y)
     * should return true if and only if y.equals(x) returns true.
     */
    @Property
    default void equalsIsSymmetric(@ForAll(ELEMENTS) E x, @ForAll(ELEMENTS) E y) {
        Assume.that(x != null);
        Assume.that(y != null);
        assertThat(x.equals(y)).isEqualTo(y.equals(x));
    }

    /**
     * For any non-null reference values x, y, and z, if x.equals(y)
     * returns true and y.equals(z) returns true, then x.equals(z)
     * should return true.
     */
    @Property
    default  void equalsIsTransitive(@ForAll(ELEMENTS) E x, @ForAll(ELEMENTS) E y, @ForAll(ELEMENTS) E z) {
        Assume.that(x != null);
        Assume.that(y != null);
        Assume.that(z != null);
        Assume.that(x.equals(y) && y.equals(z));
        assertThat(x.equals(z)).isTrue();
    }

    /**
     * For any non-null reference values x and y, multiple invocations
     * of x.equals(y) consistently return true  or consistently return
     * false, provided no information used in equals comparisons on
     * the objects is modified.
     */
    @Property
    default void equalsIsConsistent(@ForAll(ELEMENTS) E x, @ForAll(ELEMENTS) E y) {
        Assume.that(x != null);
        boolean alwaysTheSame = x.equals(y);

        for (int i = 0; i < 30; i++) {
            assertThat(x.equals(y)).isEqualTo(alwaysTheSame);
        }
    }

    /**
     * For any non-null reference value x, x.equals(null); should
     * return false.
     */
    @Property
    default void equalsReturnFalseOnNull(@ForAll(ELEMENTS) E x) {
        Assume.that(x != null);
        assertThat(x.equals(null)).isFalse();
    }

    /**
     * Whenever it is invoked on the same object more than once
     * the hashCode() method must consistently return the same
     * integer.
     */
    @Property
    default void hashCodeIsSelfConsistent(@ForAll(ELEMENTS) E x) {
        Assume.that(x != null);
        int alwaysTheSame = x.hashCode();

        for (int i = 0; i < 30; i++) {
            assertThat(x.hashCode()).isEqualTo(alwaysTheSame);
        }
    }

    /**
     * If two objects are equal according to the equals(Object) method,
     * then calling the hashCode method on each of the two objects
     * must produce the same integer result.
     */
    @Property
    default void hashCodeIsConsistentWithEquals(@ForAll(ELEMENTS) E x, @ForAll(ELEMENTS) E y) {
        Assume.that(x != null);
        Assume.that(x.equals(y));
        assertThat(x.hashCode()).isEqualTo(y.hashCode());
    }


    @Provide
    Arbitrary<? extends E> elements();

}
