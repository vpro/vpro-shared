package nl.vpro.util;

import java.util.Iterator;

/**
 * An iterator that can call a callback function when its iteration is finished.
 * <p>
 * It simply wraps another iterator, which is one iteration ahead on call of 'hasNext'.
 * @author Michiel Meeuwissen
 * @deprecated Use org.meeuw.util:mihxil-collections
 */
@Deprecated
public class CallbackIterator<T> extends org.meeuw.collections.CallbackIterator<T> {



    @lombok.Builder(builderClassName = "Builder")
    public CallbackIterator(Iterator<T> wrapped, Runnable callback) {
        super(wrapped, callback);
    }

}
