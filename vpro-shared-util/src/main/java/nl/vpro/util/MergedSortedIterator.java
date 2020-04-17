package nl.vpro.util;

import java.util.*;
import java.util.function.Supplier;

import com.google.common.collect.Iterators;
import com.google.common.collect.PeekingIterator;

/**
 * @author Michiel Meeuwissen
 * @since 0.32
 */
public class MergedSortedIterator<T>  extends BasicWrappedIterator<T> implements  CountedIterator<T> {

    protected MergedSortedIterator(Supplier<Long> size, Supplier<Long> totalSize, Iterator<T> iterator) {
        super(size, totalSize, null, null, iterator);
    }

    /**
     * This uses guava's {@link Iterators#mergeSorted}. This probable performs well, but will use a queue.
     *
     */
    @SafeVarargs
    public static <T> MergedSortedIterator<T> merge(Comparator<? super T> comparator, CountedIterator<T>... iterators) {
        return merge(comparator, Arrays.asList(iterators));
    }

    /**
     * This doesn't use a queue, so it is also useable with Hibernate.
     */
    public static <T> MergedSortedIterator<T> mergeInSameThread(Comparator<? super T> comparator, CountedIterator<T>... iterators) {
        return mergeInSameThread(comparator, Arrays.asList(iterators));
    }

    public static <T> MergedSortedIterator<T> merge(Comparator<? super T> comparator, Iterable<CountedIterator<T>> iterators) {
        return new MergedSortedIterator<T>(
            () -> getSize(iterators),
            () -> getTotalSize(iterators),
            Iterators.mergeSorted(iterators, comparator));
    }

    /**
     * This doesn't usea  queue, so it is also useable with Hibernate.
     */
    public static <T> MergedSortedIterator<T> mergeInSameThread(Comparator<? super T> comparator, Iterable<CountedIterator<T>> iterators) {
        return new MergedSortedIterator<T>(() -> getSize(iterators), () -> getTotalSize(iterators), new SameThreadMergingIterator<T>(comparator, iterators));
    }

    protected static <T> Long getSize(Iterable<CountedIterator<T>> iterators) {
        Long size = 0L;
        for (CountedIterator<?> c : iterators) {
            Optional<Long> s = c.getSize();
            if (s.isPresent()) {
                size += s.get();
            } else {
                size = null;
                break;
            }
        }
        return size;
    }

    protected static <T> Long getTotalSize(Iterable<CountedIterator<T>> iterators) {
        Long size = 0L;
        for (CountedIterator<?> c : iterators) {
            Optional<Long> s = c.getTotalSize();
            if (s.isPresent()) {
                size += s.get();
            } else {
                size = null;
                break;
            }
        }
        return size;
    }

    protected static class SameThreadMergingIterator<S> implements Iterator<S> {
        final List<PeekingIterator<S>> iterators;
        final Comparator<? super S> comparator;
        Optional<S> next = null;
        boolean needsFindNext = true;


        SameThreadMergingIterator(Comparator<? super S> comparator, Iterable<? extends Iterator<S>> iterators) {
            this.comparator = comparator;
            this.iterators = new ArrayList<>();
            for (Iterator<S> i : iterators) {
                this.iterators.add(Iterators.peekingIterator(i));
            }
        }

        @Override
        public boolean hasNext() {
            findNext();
            return next != null;
        }

        @Override
        public S next() {
            findNext();
            if (next == null){
                throw new NoSuchElementException();
            }
            needsFindNext = true;
            return next.orElse(null);
        }
        protected void findNext() {
            if (needsFindNext) {
                Optional<S> candidate = null;
                PeekingIterator<S> usedIterator = null;
                for (PeekingIterator<S> i : iterators) {
                    if (i.hasNext()) {
                        if (candidate == null || comparator.compare(candidate.orElse(null), i.peek()) >= 0) {
                            candidate = Optional.ofNullable(i.peek());
                            usedIterator = i;
                        }
                    }
                }
                next = candidate;
                if (usedIterator != null) {
                    usedIterator.next();
                }
                needsFindNext = false;
            }
        }
    }

}
