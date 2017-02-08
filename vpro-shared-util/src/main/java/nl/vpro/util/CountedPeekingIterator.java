package nl.vpro.util;

import com.google.common.collect.PeekingIterator;

/**
 * @author Michiel Meeuwissen
 * @since 5.1
 */
public interface CountedPeekingIterator<T> extends CountedIterator<T>, PeekingIterator<T> {
}
