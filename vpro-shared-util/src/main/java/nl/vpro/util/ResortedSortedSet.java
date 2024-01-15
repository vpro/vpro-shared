package nl.vpro.util;

import java.io.Serial;
import java.io.Serializable;
import java.util.*;
import java.util.function.Consumer;

import org.checkerframework.checker.nullness.qual.NonNull;

/**
 * A modifiable SortedSet, that wraps another collection (changes are reflected), with an explicit sort order.
 * @author Michiel Meeuwissen
 * @since 2.1
 */
public class ResortedSortedSet<T> extends AbstractSet<T> implements SortedSet<T>, Serializable {

    @Serial
    private static final long serialVersionUID = 0L;

    final Collection<T> wrapped;
    final SortedSet<T> set;
    final Consumer<T>[] addListeners;

    @SafeVarargs
    public ResortedSortedSet(Collection<T> wrapped, Comparator<T> comparator, Consumer<T>... addListeners) {
        set = new TreeSet<>(comparator);
        set.addAll(wrapped);
        this.wrapped = wrapped;
        this.addListeners = addListeners;
    }

    @SafeVarargs
    private ResortedSortedSet(Collection<T> wrapped, SortedSet<T> set, Consumer<T>... addListeners) {
        this.set = set;
        set.addAll(wrapped);
        this.wrapped = wrapped;
        this.addListeners = addListeners;
    }

    public static <S extends Comparable<?>> ResortedSortedSet<S> of(
        Collection<S> wrapped, Consumer<S>... addListener) {
        return new ResortedSortedSet<>(wrapped, new TreeSet<>(), addListener);
    }

    @SafeVarargs
    public ResortedSortedSet(SortedSet<T> set, Collection<T> wrapped, Consumer<T>... addListeners) {
        this.set = set;
        this.wrapped = wrapped;
        this.addListeners = addListeners;
    }

    @NonNull
    @Override
    public Iterator<T> iterator() {
        final Iterator<T> i = set.iterator();
        return new Iterator<T>() {

            T lastElement;
            boolean hasElementToRemove = false;

            @Override
            public boolean hasNext() {
                return i.hasNext();
            }

            @Override
            public T next() {
                lastElement = i.next();
                hasElementToRemove = true;
                return lastElement;
            }

            @Override
            public void remove() {
                i.remove();
                if (hasElementToRemove) {
                    wrapped.remove(lastElement);
                    hasElementToRemove = false;
                } else {
                    throw new IllegalStateException();
                }
            }
        };

    }

    @Override
    public int size() {
        return set.size();

    }
    @Override
    public boolean add(T element) {
        boolean result = set.add(element);
        if (result) {
            wrapped.add(element);
            for (Consumer<T> addListener : addListeners) {
                addListener.accept(element);
            }
        }
        return result;
    }

    @Override
    public Comparator<? super T> comparator() {
        return set.comparator();

    }

    @NonNull
    @Override
    public SortedSet<T> subSet(T fromElement, T toElement) {
        return new ResortedSortedSet<>(set.subSet(fromElement, toElement), wrapped);

    }

    @NonNull
    @Override
    public SortedSet<T> headSet(T toElement) {
        return new ResortedSortedSet<>(set.headSet(toElement), wrapped);
    }

    @NonNull
    @Override
    public SortedSet<T> tailSet(T fromElement) {
        return new ResortedSortedSet<>(set.tailSet(fromElement), wrapped);
    }

    @Override
    public T first() {
        return set.first();
    }

    @Override
    public T last() {
        return set.last();
    }
}
