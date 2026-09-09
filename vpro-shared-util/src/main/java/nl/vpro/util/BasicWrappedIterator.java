package nl.vpro.util;

import java.util.Collection;
import java.util.Iterator;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Supplier;


/**
 * @author Michiel Meeuwissen
 * @since 0.31
 * @deprecated Use org.meeuw.util:mihxil-collections
 */
@Deprecated
public class BasicWrappedIterator<T> extends org.meeuw.collections.BasicWrappedIterator<T> {


    public BasicWrappedIterator(Iterator<T> wrapped) {
        super(wrapped);
    }

    protected BasicWrappedIterator(
        Supplier<Long> sizeSupplier,
        Supplier<Long> totalSizeSupplier,
        Long size,
        Long totalSize,
        Iterator<T> wrapped) {
        super(sizeSupplier, totalSizeSupplier, size, totalSize, wrapped);
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
        super(wrapped);
    }

}
