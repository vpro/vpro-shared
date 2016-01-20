package nl.vpro.util;

import java.util.*;

import com.google.common.collect.Iterators;
import com.google.common.collect.PeekingIterator;

/**
 * @author Michiel Meeuwissen
 * @since 0.32
 */
public class MergedSortedIterator<T>  extends BasicWrappedIterator<T> implements  CountedIterator<T> {

    protected MergedSortedIterator(Long size, Iterator<T> iterator) {
        super(size, iterator);
    }

    /**
     * This uses guava's {@link Iterators#mergeSorted}. This probable performs well, but will use a queue.
     *
     */
    public static <T> MergedSortedIterator<T> merge(Comparator<? super T> comparator, CountedIterator<T>... iterators) {
        return new MergedSortedIterator<T>(getSize(iterators), Iterators.mergeSorted(Arrays.asList(iterators), comparator));
    }

    /**
     * This doesn't use queue, so it is also useable with Hibernate.
     */
    public static <T> MergedSortedIterator<T> mergeInSameThread(Comparator<? super T> comparator, CountedIterator<T>... iterators) {
        return new MergedSortedIterator<T>(getSize(iterators), new SameThreadMergingIterator<T>(comparator, iterators));
    }

    protected static Long getSize(CountedIterator<?>... iterators) {
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

    protected static class SameThreadMergingIterator<S> implements Iterator<S> {
        final List<PeekingIterator<S>> iterators;
        final Comparator<? super S> comparator;
        Optional<S> next = null;
        boolean needsFindNext = true;


        SameThreadMergingIterator(Comparator<? super S> comparator, Iterator<S>... iterators) {
            this.comparator = comparator;
            this.iterators = new ArrayList<>(iterators.length);
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
