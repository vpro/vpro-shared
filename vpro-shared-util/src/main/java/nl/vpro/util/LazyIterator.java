package nl.vpro.util;


import java.util.Iterator;
import java.util.function.Supplier;

/**
 * Wraps a supplier around an iterator. This way you can delay the instantiation of the actual iterator until the first call
 * of hasNext() or next().
 * @author Michiel Meeuwissen
 * @deprecated
 */
@Deprecated
public class LazyIterator<T> extends org.meeuw.collections.LazyIterator<T> {

    public LazyIterator(Supplier<Iterator<T>> supplier) {
        super(supplier);
    }
}
