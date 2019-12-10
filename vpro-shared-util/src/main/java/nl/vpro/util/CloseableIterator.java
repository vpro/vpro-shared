package nl.vpro.util;

import java.util.*;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

/**
 * An iterator that is also {@link AutoCloseable}.
 * @author Michiel Meeuwissen
 * @since 1.1
 */
public interface CloseableIterator<T> extends Iterator<T>, AutoCloseable {


    static void closeQuietly(AutoCloseable closeable) {
        try {
            if (closeable != null) {
                closeable.close();
            }
        } catch (Exception e) {
            // ignore
        }
    }

    /**
     * @since 2.9
     */
    static <T> CloseableIterator<T> empty() {
        return new CloseableIterator<T>() {
            @Override
            public void close() {
            }
            @Override
            public boolean hasNext() {
                return false;
            }
            @Override
            public T next() {
                throw new NoSuchElementException();
            }
        };
    }

    /**
     * <p>Morphs an existing {@link Iterator} into a {@link CloseableIterator}.</p>
     *
     * <p>
     * It it is already a {@link CloseableIterator} it will be returned unchanged.
     * If it implements {@link AutoCloseable} then its {@link AutoCloseable#close()} method will be called.
     * If not then the {@link #close()} method will do nothing.
     * </p>
     *
     * @since 2.9
     */
    @SuppressWarnings("unchecked")
    static <T> CloseableIterator<T> of(final Iterator<T> iterator) {
         if (iterator instanceof CloseableIterator) {
            return (CloseableIterator) iterator;
        } else {
            return  new CloseableIterator<T>() {
                @Override
                public void close() throws Exception {
                    if (iterator instanceof AutoCloseable) {
                        ((AutoCloseable) iterator).close();
                    }
                }
                @Override
                public boolean hasNext() {
                    return iterator.hasNext();
                }
                @Override
                public T next() {
                    return iterator.next();
                }
            };
        }
    }

    default  Stream<T> stream() {
        return StreamSupport.stream(
            Spliterators.spliteratorUnknownSize(this, Spliterator.ORDERED),
            false);

    }



}
