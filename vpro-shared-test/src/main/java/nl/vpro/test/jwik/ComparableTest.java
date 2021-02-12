package nl.vpro.test.jwik;

import net.jqwik.api.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * @author Michiel Meeuwissen
 * @since 2.22
 */
public interface ComparableTest<E extends Comparable<E>> extends BasicObjectTest<E> {
    @Property
    default void  equalsConsistentWithComparable(@ForAll(ELEMENTS) E x, @ForAll(ELEMENTS) E y) {
        Assume.that(x != null);
        Assume.that(y != null);
        assertThat(x.compareTo(y) == 0).isEqualTo(x.equals(y));
    }

    @Property
    default void compareToNull(@ForAll(ELEMENTS) E x) {
        Assume.that(x != null);
        assertThatThrownBy(() -> {
            x.compareTo(null);
        }).isInstanceOf(NullPointerException.class);
    }
}
