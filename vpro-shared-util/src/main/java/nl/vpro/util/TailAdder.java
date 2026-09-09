package nl.vpro.util;

import java.util.*;
import java.util.concurrent.Callable;
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


    @lombok.Builder(builderClassName = "Builder")
    private TailAdder(Iterator<T> wrapped, boolean onlyIfEmpty, boolean onlyIfNotEmpty, @lombok.Singular  List<Function<T, T>> adders) {
        super(wrapped, onlyIfEmpty, onlyIfNotEmpty, adders);
    }


    @SafeVarargs
    @Deprecated
    public TailAdder(Iterator<T> wrapped, boolean onlyIfEmpty, Callable<T>... adder) {
        super(wrapped, onlyIfEmpty, adder);
    }



    @Deprecated
    public TailAdder(Iterator<T> wrapped, Callable<T> adder) {
        this(wrapped, false, adder);
    }


}
