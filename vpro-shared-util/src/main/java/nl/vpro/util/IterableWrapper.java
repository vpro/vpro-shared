package nl.vpro.util;

import java.util.Iterator;
import java.util.List;
import java.util.stream.Stream;

/**
 * An object that wraps a list of values, but deliberately is not an {@link Iterable} itself.
 * <p>
 * Implementing {@link Iterable} makes e.g. Hibernate Validator consider the object a 'container'. Then {@code @Valid} on a
 * property of that type means 'validate the elements' (and logs HV000271, deprecated usage), rather than 'validate this bean'.
 * Use this interface instead when the object has properties of its own besides its elements.
 * <p>
 * Things can still be iterated using {@link #asList()} or {@link #stream()}.
 *
 * @author Michiel Meeuwissen
 * @since 5.20.3
 * @see "nl.vpro.jackson2.IterableWrapperJson and nl.vpro.jackson3.IterableWrapperJson"
 */
public interface IterableWrapper<T> {

    /**
     * The wrapped values, as a list.
     */
    List<T> asList();

    default Iterator<T> iterator() {
        return asList().iterator();
    }

    default Stream<T> stream() {
        return asList().stream();
    }

    default int size() {
        return asList().size();
    }

    default boolean isEmpty() {
        return asList().isEmpty();
    }

    default T get(int index) {
        return asList().get(index);
    }
}
