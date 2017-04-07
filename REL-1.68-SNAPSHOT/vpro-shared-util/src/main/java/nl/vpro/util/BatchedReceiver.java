package nl.vpro.util;

import lombok.Builder;
import lombok.ToString;

import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.function.BiFunction;

/**
 * Given some API which supplies only 'batched' retrieval (so with offset and max/batchsize parameters),
 * access such an API as an iterator to visit all elements.
 *
 * @author Michiel Meeuwissen
 * @since 1.68
 */
@ToString
public class BatchedReceiver<T> implements Iterator<T> {

	final int batchSize;

	final BiFunction<Long, Integer, Iterator<T>> batchGetter;

	long offset;
	Iterator<T> subIterator;
	Boolean hasNext;
	T next;


	@Builder
	public BatchedReceiver(
			Integer batchSize,
			Long offset,
			BiFunction<Long, Integer, Iterator<T>> batchGetter) {
		this.batchSize = batchSize == null ? 100 : batchSize;
		this.batchGetter = batchGetter;
		this.offset = offset == null ? 0L : offset;
	}



	@Override
	public boolean hasNext () {
		findNext();
		return hasNext;
	}

	@Override
	public T next () {
		findNext();
		if (!hasNext) {
			throw new NoSuchElementException();
		}
		hasNext = null;
		return next;
	}

	protected void findNext() {
		if (hasNext == null) {
			if (subIterator == null) {
				subIterator = batchGetter.apply(offset, batchSize);
			}
			if (subIterator.hasNext()) {
				next = subIterator.next();
				hasNext = true;
			} else {
				offset += batchSize;
				subIterator = batchGetter.apply(offset, batchSize);
				hasNext = subIterator.hasNext();
				if (hasNext) {
					next = subIterator.next();
				}
			}
		}
	}

}
