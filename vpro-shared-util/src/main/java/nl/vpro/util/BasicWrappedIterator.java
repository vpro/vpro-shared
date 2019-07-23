package nl.vpro.util;

import java.util.Collection;
import java.util.Iterator;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Supplier;

/**
 * @author Michiel Meeuwissen
 * @since 0.31
 */
public class BasicWrappedIterator<T> extends WrappedIterator<T, T> {

    private final Supplier<Long> size;
    private final Supplier<Long> totalSize;

    private long count;

    public BasicWrappedIterator(Iterator<T> wrapped) {
        super(wrapped);
        size = null;
        totalSize = null;
    }

    @lombok.Builder(builderClassName = "Builder")
    protected BasicWrappedIterator(
        Supplier<Long> sizeSupplier,
        Supplier<Long> totalSizeSupplier,
        Long size,
        Long totalSize,
        Iterator<T> wrapped) {
        super(wrapped);
        this.size = sizeSupplier != null ? sizeSupplier : () -> size;
        this.totalSize = totalSizeSupplier != null ? totalSizeSupplier :  () -> totalSize;
    }


    public BasicWrappedIterator(Long size, Long totalSize, Iterator<T> wrapped) {
        this(null, null, size, totalSize, wrapped);
    }

    public BasicWrappedIterator(AtomicLong size, AtomicLong totalSize, Iterator<T> wrapped) {
        this(size::get, totalSize::get, null, null, wrapped);
    }

    public BasicWrappedIterator(Long totalSize, Iterator<T> wrapped) {
        this(totalSize, totalSize, wrapped);
    }

    public BasicWrappedIterator(AtomicLong totalSize, Iterator<T> wrapped) {
        this(totalSize, totalSize, wrapped);
    }

    public BasicWrappedIterator(Collection<T> wrapped) {
        super(wrapped.iterator());
        this.size = () -> (long) wrapped.size();
        this.totalSize = () -> (long) wrapped.size();
    }

    @Override
    public Optional<Long> getSize() {
        if (size != null) {
            return Optional.ofNullable(size.get());
        } else {
            return super.getSize();
        }
    }

    @Override
    public Optional<Long> getTotalSize() {
        if (totalSize != null) {
            return Optional.ofNullable(totalSize.get());
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
