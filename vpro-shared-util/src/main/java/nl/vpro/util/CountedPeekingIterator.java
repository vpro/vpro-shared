package nl.vpro.util;

import com.google.common.collect.PeekingIterator;

/**
 * A {@link PeekingIterator} that is also {@link CountedIterator}.
 * @author Michiel Meeuwissen
 * @since 5.1
 * @deprecated Use org.meeuw.util:mihxil-collections
 */
@Deprecated
public interface CountedPeekingIterator<T> extends CountedIterator<T>, CloseablePeekingIterator<T> {
}
