package nl.vpro.util;

import java.util.Collection;
import java.util.Iterator;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;

/**
 * @author Michiel Meeuwissen
 * @since 0.31
 */
public class BasicWrappedIterator<T> extends WrappedIterator<T, T> {

    private final Optional<AtomicLong> size;
    private final Optional<AtomicLong> totalSize;

    private long count;

    public BasicWrappedIterator(Iterator<T> wrapped) {
        super(wrapped);
        size = Optional.empty();
        totalSize = Optional.empty();
    }

    public BasicWrappedIterator(Long size, Long totalSize, Iterator<T> wrapped) {
        super(wrapped);
        this.size = size == null ? Optional.empty() : Optional.of(new AtomicLong(size));
        this.totalSize = totalSize == null ? Optional.empty() : Optional.of(new AtomicLong(totalSize));
    }

    public BasicWrappedIterator(AtomicLong size, AtomicLong totalSize, Iterator<T> wrapped) {
        super(wrapped);
        this.size = Optional.ofNullable(size);
        this.totalSize = Optional.ofNullable(totalSize);
    }

    public BasicWrappedIterator(Long totalSize, Iterator<T> wrapped) {
        this(totalSize, totalSize, wrapped);
    }

    public BasicWrappedIterator(AtomicLong totalSize, Iterator<T> wrapped) {
        this(totalSize, totalSize, wrapped);
    }

    public BasicWrappedIterator(Collection<T> wrapped) {
        super(wrapped.iterator());
        this.size = Optional.of(new AtomicLong(wrapped.size()));
        this.totalSize = Optional.of(new AtomicLong(wrapped.size()));
    }

    @Override
    public Optional<Long> getSize() {
        if (size.isPresent()) {
            return size.map(AtomicLong::longValue);
        } else {
            return super.getSize();
        }
    }

    @Override
    public Optional<Long> getTotalSize() {
        if (totalSize.isPresent()) {
            return totalSize.map(AtomicLong::longValue);
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
