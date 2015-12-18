package nl.vpro.util;

import java.util.Collection;
import java.util.Iterator;
import java.util.Optional;

/**
 * @author Michiel Meeuwissen
 * @since 0.31
 */
public class BasicWrappedIterator<T> extends WrappedIterator<T, T> {

    private final Optional<Long> size;
    public BasicWrappedIterator(Iterator<T> wrapped) {
        super(wrapped);
        size = Optional.empty();
    }

    public BasicWrappedIterator(Long size, Iterator<T> wrapped) {
        super(wrapped);
        this.size = Optional.ofNullable(size);
    }


    public BasicWrappedIterator(Collection<T> wrapped) {
        super(wrapped.iterator());
        this.size = Optional.of((long) wrapped.size());
    }

    @Override
    public T next() {
        return wrapped.next();

    }
}
