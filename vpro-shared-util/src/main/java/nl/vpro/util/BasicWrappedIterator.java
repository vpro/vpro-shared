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
    private final Optional<Long> totalSize;

    private long count;

    public BasicWrappedIterator(Iterator<T> wrapped) {
        super(wrapped);
        size = Optional.empty();
        totalSize = Optional.empty();
    }

    public BasicWrappedIterator(Long size, Long totalSize, Iterator<T> wrapped) {
        super(wrapped);
        this.size = Optional.ofNullable(size);
        this.totalSize = Optional.ofNullable(totalSize);
    }


    public BasicWrappedIterator(Long totalSize, Iterator<T> wrapped) {
        this(totalSize, totalSize, wrapped);
    }

    public BasicWrappedIterator(Collection<T> wrapped) {
        super(wrapped.iterator());
        this.size = Optional.of((long) wrapped.size());
        this.totalSize = Optional.of((long) wrapped.size());
    }

    @Override
    public Optional<Long> getSize() {
        if (size.isPresent()) {
            return size;
        } else {
            return super.getSize();
        }
    }

    @Override
    public Optional<Long> getTotalSize() {
        if (totalSize.isPresent()) {
            return totalSize;
        } else {
            return super.getTotalSize();
        }
    }

    @Override
    public T next() {
        count++;
        return wrapped.next();
    }

    @Override
    public Long getCount() {
        return count;
    }
}
