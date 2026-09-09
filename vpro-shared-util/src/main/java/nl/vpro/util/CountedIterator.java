package nl.vpro.util;

/**
 * An iterator that is also aware of the current position {@link #getCount()}, and optionally of the size of the object that is iterated {@link #getSize()}, and also optionally of a 'total' size (in case this iterator presents some sub-collection) {@link #getTotalSize()}.
 *
 * @author Michiel Meeuwissen
 * @since 0.31
 * @deprecated Use org.meeuw.util:mihxil-collections
 */
@Deprecated
public interface CountedIterator<T> extends org.meeuw.collections.CountedIterator<T> {



}

