package nl.vpro.util;

import java.util.Iterator;


/**
 * @author Michiel Meeuwissen
 * @since 1.3
 * @deprecated Use org.meeuw.util:mihxil-collections
 */
@Deprecated
public  abstract class WrappedIterator<T, S> extends org.meeuw.collections.WrappedIterator<T, S> {
    public WrappedIterator(Iterator<T> wrapped) {
        super(wrapped);
    }
}
