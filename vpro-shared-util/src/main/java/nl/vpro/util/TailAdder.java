package nl.vpro.util;

import java.util.Iterator;
import java.util.List;
import java.util.function.Function;


/**
 * Adapts an existing iterator, to add elements at the end, perhaps based on the last element.
 *
 * @author Michiel Meeuwissen
 * @since 1.17
 * @deprecated Use org.meeuw.util:mihxil-collections
 */
@Deprecated
public class TailAdder<T> extends org.meeuw.collections.TailAdder<T> {


    @lombok.Builder(builderClassName = "Builder",
        builderMethodName = "_builder",
        buildMethodName = "_build"
    )
    private TailAdder(Iterator<T> wrapped,
                      boolean onlyIfEmpty,
                      boolean onlyIfNotEmpty,
                      @lombok.Singular  List<? extends Function<T, T>> adders
                      ) {
        super(wrapped, onlyIfEmpty, onlyIfNotEmpty, adders.toArray(new Function[0]));
    }





}
