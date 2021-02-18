package nl.vpro.util;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

/**
 * @author Michiel Meeuwissen
 */
class CloseableIteratorTest {

    @Test
    void closeQuietly() {
        AtomicInteger count = new AtomicInteger(0);
        CloseableIterator.closeQuietly(null,
            () -> {
                throw new IOException();
            },
            count::incrementAndGet);

        assertThat(count.get()).isEqualTo(1);
    }

    @Test
    void empty() {
        CloseableIterator<String> empty = CloseableIterator.empty();
        assertThat(empty.hasNext()).isFalse();
        assertThatThrownBy(empty::next).isInstanceOf(NoSuchElementException.class);
        assertThatNoException().isThrownBy(empty::close);
    }

    @Test
    void peeking() {
        Impl i = new Impl();
        CloseablePeekingIterator<String> peeking = CloseableIterator.peeking(i);
        assertThat(peeking.peek()).isEqualTo("a");
        assertThat(peeking.stream()).contains("a", "b", "c");

        assertThat(CloseableIterator.peeking(null)).isNull();

    }

    @Test
    void stream() {
        Impl i = new Impl();
        try (Stream<String> stream = i.stream().limit(2)) {
            assertThat(stream).contains("a", "b");
        }
        assertThat(i.closed.get()).isEqualTo(1);
    }

    private static class Impl implements  CloseableIterator<String> {
        List<String> list = Arrays.asList("a", "b", "c");
        AtomicInteger closed = new AtomicInteger(0);
        private final Iterator<String> wrapped = list.iterator();
        @Override
        public void close() throws Exception {
            closed.incrementAndGet();
        }

        @Override
        public boolean hasNext() {
            return wrapped.hasNext();
        }

        @Override
        public String next() {
            return wrapped.next();
        }
    };
}
