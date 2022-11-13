package nl.vpro.util;

import java.util.*;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import com.google.common.collect.PeekingIterator;

/**
 * An iterator that is also {@link AutoCloseable}.
 * @author Michiel Meeuwissen
 * @since 1.1
 */
public interface CloseableIterator<T> extends Iterator<T>, AutoCloseable {


    static void closeQuietly(AutoCloseable... closeables) {
        for (AutoCloseable closeable : closeables) {
            try {
                if (closeable != null) {
                    closeable.close();
                }
            } catch (Exception e) {
                // ignore
            }
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
     * If it is already a {@link CloseableIterator} it will be returned unchanged.
     * If it implements {@link AutoCloseable} then its {@link AutoCloseable#close()} method will be called.
     * If not then the {@link #close()} method will do nothing.
     * </p>
     *
     * @since 2.9
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    static <T> CloseableIterator<T> of(final Iterator<T> iterator) {
        if (iterator instanceof CloseableIterator) {
            return (CloseableIterator) iterator;
        } else if (iterator instanceof PeekingIterator) {
            return new WrappedPeekingCloseableIterator<>((PeekingIterator<T>) iterator);
        } else {
            return new WrappedCloseableIterator<>(iterator);
        }
    }

    static <S> CloseablePeekingIterator<S> peeking(CloseableIterator<S> wrapped){
        return wrapped == null ? null : wrapped.peeking();
    }

    default  Stream<T> stream() {
        return StreamSupport.stream(
            Spliterators.spliteratorUnknownSize(this, Spliterator.ORDERED),
            false).onClose(() -> {
            try {
                this.close();
            } catch (Exception exception) {
                throw new RuntimeException(exception);
            }
        });

    }

    /**
     * If you need a guava {@link PeekingIterator}, this will make you one. It remains also a {@link CloseableIterator}
     */
    default CloseablePeekingIterator<T> peeking() {
        return new CloseablePeekingIteratorImpl<>(this);
    }

    class WrappedCloseableIterator<S> implements CloseableIterator<S> {
        protected final Iterator<S> iterator;

        protected WrappedCloseableIterator(Iterator<S> iterator) {
            this.iterator = iterator;
        }

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
        public S next() {
            return iterator.next();
        }
        @Override
        public void remove() {
            iterator.remove();
        }
        @Override
        public String toString() {
            return "Closeable[" + iterator + "]";
        }
    }

    class WrappedPeekingCloseableIterator<S> extends WrappedCloseableIterator<S> implements PeekingIterator<S> {

        protected WrappedPeekingCloseableIterator(PeekingIterator<S> iterator) {
            super(iterator);
        }

        @Override
        public S peek() {
            return ((PeekingIterator<S>) iterator).peek();
        }
    }

}
