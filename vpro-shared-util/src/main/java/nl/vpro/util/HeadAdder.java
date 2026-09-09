package nl.vpro.util;

import lombok.extern.slf4j.Slf4j;

import java.util.Iterator;
import java.util.List;
import java.util.function.Function;


/**
 * Wraps an iterator, to add zero or more elements at the start of it.
 * @author Michiel Meeuwissen
 * @since 1.72
 * @deprecated Use org.meeuw.util:mihxil-collections
 */
@Slf4j
@Deprecated
public class HeadAdder<T> extends org.meeuw.collections.HeadAdder<T>  {




    @SuppressWarnings("unchecked")
    @lombok.Builder(builderClassName = "Builder")
    private HeadAdder(Iterator<T> wrapped, final boolean onlyIfEmpty, final boolean onlyIfNotEmpty, @lombok.Singular  List<Function<T, T>> adders) {
        super(wrapped, onlyIfEmpty, onlyIfNotEmpty, adders);
    }

}
