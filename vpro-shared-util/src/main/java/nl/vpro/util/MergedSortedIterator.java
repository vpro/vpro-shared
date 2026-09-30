package nl.vpro.util;

import java.util.*;
import java.util.function.Supplier;

import org.meeuw.collections.CountedIterator;

/**
 * @author Michiel Meeuwissen
 * @since 0.32
 * @deprecated Use org.meeuw.util:mihxil-collections
 */
@Deprecated
public class MergedSortedIterator<T>  extends org.meeuw.collections.MergedSortedIterator<T> {

    protected MergedSortedIterator(Supplier<Long> size, Supplier<Long> totalSize, Iterator<T> iterator, List<CountedIterator<T>> sources) {
        super(size, totalSize, iterator, sources);
    }


}
