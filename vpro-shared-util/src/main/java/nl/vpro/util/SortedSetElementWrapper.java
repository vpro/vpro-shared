package nl.vpro.util;

import java.util.SortedSet;

/**
 * @author Michiel Meeuwissen
 * @since 2.3.1
 * @deprecated Use org.meeuw.util:mihxil-collections
 */
@Deprecated
public abstract class SortedSetElementWrapper<T, S> extends org.meeuw.collections.SortedSetElementWrapper<T, S> {

    public SortedSetElementWrapper(SortedSet<T> wrapped) {
        super(wrapped);

    }

}
