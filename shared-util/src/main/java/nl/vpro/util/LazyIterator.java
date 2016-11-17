package nl.vpro.util;


import java.util.Iterator;
import java.util.function.Supplier;

/**
 * Wrap's a supplier around an iterator. This way you can delay the instantiation of the actual iterator until the first call
 * of hasNext() or next().
 * @author Michiel Meeuwissen
 */
public class LazyIterator<T> implements Iterator<T> {

	private final Supplier<Iterator<T>> supplier;
	private Iterator<T> iterator;

	public LazyIterator(Supplier<Iterator<T>> supplier) {
		this.supplier = supplier;
	}

	@Override
	public boolean hasNext() {
		return getSupplied().hasNext();
	}

	@Override
	public T next() {
		return getSupplied().next();
	}

	private Iterator<T> getSupplied() {
		if (iterator == null) {
			iterator = supplier.get();
		}
		return iterator;
	}
}
