package nl.vpro.util;

import lombok.extern.slf4j.Slf4j;

import java.util.Iterator;
import java.util.function.*;


/**
 * Filtering, or Transforming iterator.
 * @author Michiel Meeuwissen
 * @since 1.3
 * @deprecated Use org.meeuw.util:mihxil-collections
 */
@Slf4j
@Deprecated
public class FilteringIterator<T> extends org.meeuw.collections.FilteringIterator<T>  {


    public FilteringIterator(
            Iterator<? extends T> wrapped,
            Predicate<? super T> filter) {
        super(wrapped, filter);
    }

    public FilteringIterator(
        Iterator<? extends T> wrapped,
        Predicate<? super T> filter,
        KeepAlive keepAlive) {
        super(wrapped, filter, keepAlive);
    }

    @lombok.Builder(builderClassName = "Builder")
    private FilteringIterator(
            Iterator<? extends T> wrapped,
            Predicate<? super T> filter,
            KeepAlive keepAlive,
            Consumer<org.meeuw.collections.FilteringIterator<T>> callback
            ) {
        super(wrapped, filter, keepAlive, callback);
    }


}
