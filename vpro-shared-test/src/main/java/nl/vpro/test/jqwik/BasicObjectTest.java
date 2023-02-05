package nl.vpro.test.jqwik;


import org.meeuw.util.test.BasicObjectTheory;


/**
 * Basic tests on {@link Object#equals(Object)}, {@link Object#hashCode()} and {@link Object#toString()}, which must probably be valid for _any_ override of those.
 *
 * @author Michiel Meeuwissen
 * @deprecated Just use {@link BasicObjectTheory}
 */
@Deprecated
public interface BasicObjectTest<E> extends BasicObjectTheory<E> {


}
