package nl.vpro.util;

import java.util.Iterator;
import java.util.NoSuchElementException;

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


}
