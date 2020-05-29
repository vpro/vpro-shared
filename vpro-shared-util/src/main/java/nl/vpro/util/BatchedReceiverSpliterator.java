package nl.vpro.util;

import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

import java.util.Iterator;
import java.util.Spliterator;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Given some API which supplies only 'batched' retrieval (so with offset and max/batchsize parameters),
 * access such an API as an iterator to visit all elements.
 *
 * If an API provides access to huge set of elements, they often do it with some paging mechanism, or by some 'resumption token' formalism. With {@link BatchedReceiverSpliterator} this can be morphed into a simple {@link Spliterator}.

 *
 * <h3>Paging</h3>
 * The 'batchGetter' argument should be a {@link BiFunction}, returning an iterator for the page described by given offset and batch size
 * <pre>
 * {@code
 * Spliterator<String> i = BatchedReceiverSpliterator.<String>builder()
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
 * You simply provide a {@link Supplier}. A lambda would probably not suffice because you might need the previous result the get the next one. E.g. this (using olingo code)
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
 *
 * @author Michiel Meeuwissen
 * @since 2.7
 */
@ToString
@Slf4j
public class BatchedReceiverSpliterator<T> implements Spliterator<T> {

	final Integer batchSize;

	final BiFunction<Long, Integer, Iterator<T>> batchGetter;

	long subCount = 0;
	long offset;
	Iterator<T> subIterator;
	Boolean hasNext;
	T next;


	@lombok.Builder(builderClassName = "Builder", buildMethodName = "_build")
	public BatchedReceiverSpliterator(
			Integer batchSize,
			Long offset,
			BiFunction<Long, Integer, Iterator<T>> _batchGetter) {
		this.batchSize = batchSize;
		this.batchGetter = _batchGetter;
		this.offset = offset == null ? 0L : offset;
	}

	@Override
	public boolean tryAdvance(Consumer<? super T> action) {
		findNext();
		if (hasNext) {
			action.accept(next);
			return true;
		} else {
			return false;
		}

	}

	@Override
	public Spliterator<T> trySplit() {
		return null;

	}

	@Override
	public long estimateSize() {
		return Long.MAX_VALUE;

	}

	@Override
	public int characteristics() {
		return Spliterator.IMMUTABLE;

	}

	private enum BatchType {
		BIFUNCTION,
		SUPPLIER;
	}


	public static class Builder<T>  {

		private BatchType batchType = null;

        /**
         *
         * @param batchGetter A function to get the next batch, the parameters are the current necessary offset, and batch size
         */
	    public Builder<T> batchGetter(BiFunction<Long, Integer, Iterator<T>> batchGetter) {
	    	batchType = BatchType.BIFUNCTION;
	        return _batchGetter(batchGetter);
        }

        /**
         * @param batchGetter For 'resumption token' like functionality, the offset and max argument can be irrelevant.
         */
        public Builder<T> batchGetter(final Supplier<Iterator<T>> batchGetter) {
        	batchType = BatchType.SUPPLIER;
            return _batchGetter((offset, max) -> batchGetter.get());
        }

        public BatchedReceiverSpliterator<T> build() {
        	if (_batchGetter == null) {
        		throw new IllegalStateException("No batch getter defined");
			}
        	if (batchType == BatchType.BIFUNCTION && batchSize == null) {
				log.debug("Specified a bifunction, and nobatch size. The batch size is implicetely set to 100");
        		batchSize(100);
			}
        	if (batchType == BatchType.SUPPLIER && batchSize != null) {
        		log.warn("Specified a supplier, and a batch size. The batch size is ignored");
        		batchSize(null);
			}
        	return _build();
		}

    }




	protected void findNext() {
		if (hasNext == null) {
			if (subIterator == null) {
				subIterator = batchGetter.apply(offset, batchSize);
                subCount = 0;
                if (subIterator == null) {
                    hasNext = false;
                    return;
                }
			}
			if (subIterator.hasNext()) {
				next = subIterator.next();
                subCount++;
				hasNext = true;
			} else {
				offset += subCount;
				if (batchSize == null || subCount == batchSize) {
                    subIterator = batchGetter.apply(offset, batchSize);
                } else {
				    subIterator = null;
                }
                subCount = 0;
                if (subIterator == null) {
                    hasNext = false;
                    return;
                }
				hasNext = subIterator.hasNext();
				if (hasNext) {
					next = subIterator.next();
                    subCount++;
                }
			}
		}
	}

}
