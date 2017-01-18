package nl.vpro.util;

import java.util.*;
import java.util.function.Function;
import java.util.function.Predicate;

/**
 * @author Michiel Meeuwissen
 * @since 4.3
 */
public class TransformingSortedSet<T, S> extends AbstractSet<T> implements SortedSet<T>, TransformingCollection<T, S, SortedSet<T>, SortedSet<S>> {
    private final SortedSet<S> wrapped;
    private final Map<S, Optional<T>> transformed;
    private final Function<S, T> transformer;
    private final Function<T, S> producer;

    private final Comparator<T> comparator = new Comparator<T>() {
        @Override
        public int compare (T o1, T o2){
            return wrapped.comparator().compare(producer.apply(o1), producer.apply(o2));
        }
    };

    public TransformingSortedSet(SortedSet<S> wrapped, Function<S, T> transformer, Function<T, S> producer) {
        this.wrapped = wrapped;
        transformed = new HashMap<>(wrapped.size());
        this.transformer = transformer;
        this.producer = producer;
    }



    @Override
    public Iterator<T> iterator() {
        return TransformingCollection.super.iterator();
    }

    @Override
    public int size() {
        return TransformingCollection.super.size();
    }

    @Override
    public boolean add(T toAdd) {
        return TransformingCollection.super.add(toAdd);
    }

    @Override
    public boolean remove(Object o) {
        return TransformingCollection.super.remove(o);
    }

    @Override
    public Comparator<? super T> comparator() {
        return comparator;
    }

    @Override
    public SortedSet<T> subSet(T fromElement, T toElement) {
        return new TransformingSortedSet<>(wrapped.subSet(produce(fromElement), produce(toElement)), transformer, producer);

    }

    @Override
    public SortedSet<T> headSet(T toElement) {
        return new TransformingSortedSet<>(wrapped.headSet(produce(toElement)), transformer, producer);
    }

    @Override
    public SortedSet<T> tailSet(T fromElement) {
        return new TransformingSortedSet<>(wrapped.tailSet(produce(fromElement)), transformer, producer);
    }

    @Override
    public T first() {
        return transform(wrapped.first());
    }

    @Override
    public T last() {
        return transform(wrapped.last());
    }

    @Override
    public SortedSet<S> unwrap() {
        return wrapped;
    }



    @Override
    public T transform(S entry) {
        return transformed.computeIfAbsent(entry, e -> Optional.ofNullable(transformer.apply(e))).orElse(null);
    }

    @Override
    public S produce(T entry) {
        return producer.apply(entry);

    }

    @Override
    public SortedSet<S> newWrap() {
        return new TreeSet<S>();

    }

    @Override
    public SortedSet<T> newFiltered() {
        return new TreeSet<T>();

    }


    public SortedSet<T> filter(Predicate<S> s) {
        SortedSet<T> result = new TreeSet<>();
        for (S in : unwrap()) {
            if (s.test(in)) {
                result.add(transform(in));
            }
        }
        return result;
    }


}
