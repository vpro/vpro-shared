package nl.vpro.util;

import java.io.Serializable;
import java.util.*;

import org.checkerframework.checker.nullness.qual.NonNull;

/**
 * A modifiable SortedSet, that wrapped another collection (changes are reflected), with an explicit sort order.
 * @author Michiel Meeuwissen
 * @since 2.1
 */
public class ResortedSortedSet<T> extends AbstractSet<T> implements SortedSet<T>, Serializable {

    private static final long serialVersionUID = 0L;

    final Collection<T> wrapped;
    final SortedSet<T> set;

    public ResortedSortedSet(Collection<T> wrapped, Comparator<T> comparator) {
        set = new TreeSet<>(comparator);
        set.addAll(wrapped);
        this.wrapped = wrapped;
    }

    /**
     * @deprecated Use {@link #of(Collection)} to enforce that the collection contains {@link Comparable}
     */
    @Deprecated()
    public ResortedSortedSet(Collection<T> wrapped) {
        this(wrapped, new TreeSet<>());
    }

    private ResortedSortedSet(Collection<T> wrapped, SortedSet<T> set) {
        this.set = set;
        set.addAll(wrapped);
        this.wrapped = wrapped;
    }

    public static <S extends Comparable<?>> ResortedSortedSet<S> of(
        Collection<S> wrapped) {
        return new ResortedSortedSet<S>(wrapped, new TreeSet<>());
    }

    public ResortedSortedSet(SortedSet<T> set, Collection<T> wrapped) {
        this.set = set;
        this.wrapped = wrapped;
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
