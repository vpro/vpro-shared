package nl.vpro.test.theory;

import org.junit.experimental.theories.Theory;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.fail;
import static org.junit.Assume.assumeNotNull;

/**
 * @author Michiel Meeuwissen
 * @since 1.6
 */
public abstract class ComparableTest<S extends Comparable<S>> extends ObjectTest<S> {

    @Theory
    public final <I> void  equalsConsistentWithComparable(Comparable x, Object y) {
        assumeNotNull(x);
        assumeNotNull(y);
        assertThat(x.compareTo(y) == 0).isEqualTo(x.equals(y));
    }

    @Theory
    public final void compareToNull(Comparable<?> x) {
        assumeNotNull(x);
        try {
            x.compareTo(null);
            fail("Compare to null should throw NPE");
        } catch (NullPointerException npe) {
            // ok
        }
    }
}
