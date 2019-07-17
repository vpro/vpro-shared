package nl.vpro.util;

import java.util.Collection;
import java.util.Iterator;

import org.checkerframework.checker.nullness.qual.NonNull;


/**
 * @author Michiel Meeuwissen
 * @since 4.3
 */
public interface TransformingCollection<T, S, U extends Collection<T>, V extends Collection<S>> extends Collection<T> {

    T transform(S entry);

    default T transform(int index, S entry) {
        return transform(entry);
    }


    S produce(T entry);

    V newWrap();

    U newFiltered();

    V unwrap();


    @NonNull
    @Override
    default Iterator<T> iterator() {
        final Iterator<S> i = unwrap().iterator();

        return new Iterator<T>() {
            int counter = 0;
            @Override
            public boolean hasNext() {
                return i.hasNext();
            }

            @Override
            public T next() {
                return transform(counter++, i.next());
            }

            @Override
            public void remove() {
                i.remove();
            }
        };
    }

    @Override
    default int size() {
        return unwrap().size();
    }


    @Override
    default boolean add(T toAdd) {
        return unwrap().add(produce(toAdd));
    }

    @Override
    default boolean remove(Object toRemove) {
        return unwrap().remove(produce((T) toRemove));
    }


    default U filter() {
        U result = newFiltered();
        result.addAll(this);
        return result;
    }

    default V produce() {
        V result = newWrap();
        for (T t : this) {
            result.add(produce(t));
        }
        return result;
    }

}
