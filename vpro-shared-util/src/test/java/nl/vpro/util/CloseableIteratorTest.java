package nl.vpro.util;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * @author Michiel Meeuwissen
 */
class CloseableIteratorTest {



    @Test
    void stream() {
        Impl i = new Impl();
        try (Stream<String> stream = i.stream().limit(2)) {
            assertThat(stream).contains("a", "b");
        }
        assertThat(i.closed.get()).isEqualTo(1);
    }
    @Test
    void streamCloseThrows() {
        Impl i = new Impl(true);
        assertThatThrownBy(() -> {
            try (Stream<String> stream = i.stream().limit(2)) {
                assertThat(stream).contains("a", "b");
            }
        }).isInstanceOf(Exception.class);
        assertThat(i.closed.get()).isEqualTo(1);
    }



    private static class Impl implements  CloseableIterator<String> {
        List<String> list = Arrays.asList("a", "b", "c");
        AtomicInteger closed = new AtomicInteger(0);
        private final Iterator<String> wrapped = list.iterator();
        private final boolean closeThrows;
        public Impl(boolean t) {
            this.closeThrows = t;
        }
        public Impl() {
            this(false);
        }

        @Override
        public void close() throws Exception {
            closed.incrementAndGet();
            if (closeThrows) {
                throw new Exception("fooobar");
            }
        }

        @Override
        public boolean hasNext() {
            return wrapped.hasNext();
        }

        @Override
        public String next() {
            return wrapped.next();
        }
    }

    private static class Impl2 implements  Iterator<String>, AutoCloseable {
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
    }
}
