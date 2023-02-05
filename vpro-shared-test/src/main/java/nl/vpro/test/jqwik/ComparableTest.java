package nl.vpro.test.jqwik;

import org.meeuw.util.test.ComparableTheory;

/**
 * @author Michiel Meeuwissen
 * @since 2.22
 * @deprecated Just use {@link ComparableTheory}
 */
@Deprecated
public interface ComparableTest<E extends Comparable<E>> extends ComparableTheory<E> {


}
