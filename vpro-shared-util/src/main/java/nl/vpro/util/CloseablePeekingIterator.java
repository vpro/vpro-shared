package nl.vpro.util;

import com.google.common.collect.PeekingIterator;

/**
 * @author Michiel Meeuwissen
 * @since 2.21
 * @deprecated Use org.meeuw.util:mihxil-collections
 */
@Deprecated
public interface CloseablePeekingIterator<E> extends PeekingIterator<E>, CloseableIterator<E> {
}
