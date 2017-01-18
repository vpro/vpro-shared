package nl.vpro.util;

import java.util.*;
import java.util.function.Function;

/**
 * @author Michiel Meeuwissen
 * @since 4.3
 */
public class TransformingList<T, S> extends AbstractList<T> implements TransformingCollection<T, S, List<T>, List<S>> {

    private List<S> wrapped;
    private final List<Optional<T>> transformed;
    private final Function<S, T> transformer;
    private final Function<T, S> producer;

    public TransformingList(List<S> wrapped, Function<S, T> transformer, Function<T, S> producer) {
        this.wrapped = wrapped;
        this.transformed = new ArrayList<>(Collections.nCopies(wrapped.size(), null));
        this.transformer = transformer;
        this.producer = producer;
    }


    @Override
    public T get(int index) {
        return transform(index, wrapped.get(index));
    }

    @Override
    public int size() {
        return TransformingCollection.super.size();
    }

    @Override
    public boolean remove(Object o) {
        return TransformingCollection.super.remove(o);
    }

    @Override
    public T set(int i, T toSet) {
        S newObject = wrapped.set(i, produce(toSet));
        return transform(i, newObject);
    }

    @Override
    public void add(int i, T toAdd) {
        transformed.add(i, null);
        wrapped.add(i, produce(toAdd));
    }

    @Override
    public T remove(int i) {
        S removed = wrapped.remove(i);
        transformed.remove(i);
        return transform(removed);
    }

    @Override
    public T transform(S entry) {
        return transformer.apply(entry);
    }

    @Override
    public T transform(int i, S entry) {
        Optional<T> trans = transformed.get(i);
        if (trans == null) {
            trans = Optional.ofNullable(transformer.apply(entry));
            transformed.set(i, trans);
        }
        return trans.orElse(null);
    }

    @Override
    public S produce(T entry) {
        return producer.apply(entry);

    }

    @Override
    public List<S> newWrap() {
        return new ArrayList<S>();

    }

    @Override
    public List<T> newFiltered() {
        return new ArrayList<T>();

    }

    @Override
    public List<S> unwrap() {
        return wrapped;
    }

}
