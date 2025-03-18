package nl.vpro.util;

import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

import java.util.*;
import java.util.function.*;

/**
 * Given some API which supplies only 'batched' retrieval (so e.g. with offset and max/batchsize parameters),
 * access such an API as an iterator to visit all elements.
 * <p>
 * If an API provides access to huge set of elements, they often do it with some paging mechanism, or by some 'resumption token' formalism. With {@link BatchedReceiver} this can be morphed into a simple {@link java.util.Iterator}.
 *
 *
 * <h3>Paging</h3>
 * The 'batchGetter' argument should be a {@link java.util.function.BiFunction}, returning an iterator for the page described by given offset and batch size
 * <pre>
 * {@code
 * Iterator<String> i = BatchedReceiver.<String>builder()
 *     .batchGetter((offset, max) ->
 *        apiClient.getPage(offset, max).iterator()
 *     )
 *     .batchSize(6)
 *     .build();
 * i.forEachRemaining(string -> {
 *       ...<do stuff...>
 *   });
 * }</pre>
 * <h3>Resumption token formalism</h3>
 * You simply provide a {@link java.util.function.Supplier}. A lambda would probably not suffice because you might need the previous result the get the next one. E.g. this (using olingo code)
 * <h4>Just use a supplier</h4>
 * <pre>
 * {@code
 *    public Iterator<ClientEntity> iterate(URIBuilder ub) {
 *         return BatchedReceiver.<ClientEntity>builder()
 *             .batchGetter(new Supplier<Iterator<ClientEntity>>() {
 *                 ClientEntitySet result;
 *                 @Override
 *                 public Iterator<ClientEntity> get() {
 *                     if (result != null) {
 *                         result = query(result.getNext());
 *                     } else {
 *                         result = query(ub);
 *                     }
 *                     return result.getEntities().iterator();
 *                 }
 *             })
 *             .build();
 *     }
 * }
 * </pre>
 * <h4>An initial supplier and a 'next page'</h4>
 * This case could actually be simplified like so
 * <pre>
 *  {@code
 *      public Iterator<ClientEntity> iterate(URIBuilder ub) {
 *           return BatchedReceiver.<ClientEntity>builder()
 *               .initialAndResumption(
 *                     () -> query(ub),
 *                     (result) -> query(result.getNext()),
 *                     (result) -> result.getEntities().iterator()
 *               .build();
 *       }
 *   }
 * </pre>
 * If the result is an {@link Iterable} itself, it can be
 * <pre>
 * @{code
 *  public Iterator<ClientEntity> iterate(URIBuilder ub) {
 *             return BatchedReceiver.<ClientEntity>builder()
 *                 .initialAndResumption(
 *                       () -> query(ub),
 *                       (result) -> query(result.getNext())
 *                 .build();
 * }
 * </pre>
 *
 *
 *
 * @author Michiel Meeuwissen
 * @since 1.68
 */
@ToString
@Slf4j
public class BatchedReceiver<T> implements Iterator<T> {

    /**
     * Supplies the next iterator.
     */
    final Supplier<Optional<Iterator<T>>> supplier;

    /**
     * The count in the current batch
     */
    long subCount = 0;

    /**
     * The current offset
     */
    long offset;

    /**
     * The currently active iterator
     */
    Iterator<T> subIterator;

    Boolean hasNext;
    T next;


    /**
     * @param offset The initial offset, defaults to 0
     */
    @lombok.Builder(
        builderClassName = "Builder",
        buildMethodName = "_build")
    private BatchedReceiver(
        Long offset,
        Supplier<Optional<Iterator<T>>> supplier) {
        this.supplier = supplier;
        this.offset = offset == null ? 0L : offset;
    }




    @Override
    public boolean hasNext() {
        findNext();
        return hasNext;
    }

    @Override
    public T next() {
        findNext();
        if (!hasNext) {
            throw new NoSuchElementException();
        }
        hasNext = null;
        return next;
    }

    protected void findNext() {
        while (hasNext == null) {
            if (subIterator == null) {
                Optional<Iterator<T>> optionalNewIterator = supplier.get();
                subCount = 0;
                if (optionalNewIterator.isEmpty()) {
                    hasNext = false;
                    return;
                } else {
                    subIterator = optionalNewIterator.get();
                }
            }
            if (subIterator.hasNext()) {
                next = subIterator.next();
                subCount++;
                offset++;
                hasNext = true;
                return;
            } else {
                hasNext = null;
                subIterator = null;
            }
        }
    }

    public static class Builder<T> {

        private Integer batchSize = null;
        private BiFunction<Long, Integer, Iterator<T>> batchGetter;


        /**
         * For paging with 'resumption tokens' it is convenient to have
         * multiple paths.
         * <p>
         * See {@link #initialAndResumption(Supplier, Function)} if the received objects are iterable themselves,
         * in which case two parameter suffice.
         *
         * @param initial A supplier to get the object representing the first batch
         * @param resumption A function to get the next batch from the previous one
         * @param getter A function to get the iterator from the object representing the batch
         * @since 5.6
         */
        public <X> Builder<T> initialAndResumption(
            Supplier<X> initial,
            Function<X, Optional<X>> resumption,
            Function<X, Iterator<T>> getter) {

            return supplier(new Supplier<>() {
                X holder = null;

                @Override
                public Optional<Iterator<T>> get() {
                    if (holder == null) {
                        holder = initial.get();
                    } else {
                        holder = resumption.apply(holder).orElse(null);
                        if (holder == null) {
                            return Optional.empty();
                        }
                    }
                    return Optional.of(getter.apply(holder));
                }
            });
        }

        /**
         * @param initial A supplier to get the {@link Iterable} representing the first batch
         * @param resumption A function to get the next batch from the previous one
         * @see #initialAndResumption(Supplier, Function, Function)
         * @since 5.6
         */
        public <X extends Iterable<T>> Builder<T> initialAndResumption(
            Supplier<X> initial,
            Function<X, Optional<X>> resumption) {
            return initialAndResumption(
                initial,
                resumption,
                Iterable::iterator);
        }


        /**
         * @param batchGetter A function to get the next batch by offset and batch size, the parameters are the current necessary offset, and batch size
         */
        public Builder<T> batchGetter(BiFunction<Long, Integer, Iterator<T>> batchGetter) {
            this.batchGetter = batchGetter;
            return this;
        }

        /**
         * @param batchGetter For 'resumption token' like functionality, the offset and max argument can be irrelevant.
         */
        public Builder<T> batchGetter(final Supplier<Iterator<T>> batchGetter) {
            return batchGetter((offset, max) -> batchGetter.get());
        }

        public Builder<T> batchSize(int batchSize) {
            this.batchSize = batchSize;
            return this;
        }

        public BatchedReceiver<T> build() {
            if (batchGetter != null) {
                if (supplier != null) {
                    throw new IllegalStateException("Both batchGetter and supplier are defined");
                }
                if (batchSize == null) {
                    log.debug("Specified a bifunction, and no batch size. The batch size is implicitly set to 100");
                    batchSize(100);
                }

                Supplier<Iterator<T>> supplier = new Supplier<Iterator<T>>() {
                    Iterator<T> it = null;
                    @Override
                    public Iterator<T> get() {
                        it = batchGetter.apply(offset, batchSize);
                        if (!it.hasNext()) {
                            return null;
                        }
                        return it;
                    }
                };

                return
                    supplier(() -> Optional.ofNullable(supplier.get())
                    )._build();
            }
            if (batchSize != null) {
                 throw new IllegalStateException("Specifying batch size only makes sense with a batchGetter");
            }
            if (supplier == null) {
                throw new IllegalStateException("No supplier defined");
            }
            return _build();
        }

    }


}
