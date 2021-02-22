package nl.vpro.util;

import java.util.*;
import java.util.concurrent.atomic.AtomicLong;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * @author Michiel Meeuwissen

 */
class BasicWrappedIteratorTest {
    private final List<String> list = Arrays.asList("a", "b", "c");


    @Test
    public void wrap() {
        BasicWrappedIterator<String> i = new BasicWrappedIterator<>(list.iterator());
        assertThat(i.getSize()).isEmpty();
        assertThat(i.getTotalSize()).isEmpty();
        test(i);
    }

    @Test
    public void wrapWithLong() {
        BasicWrappedIterator<String> i = new BasicWrappedIterator<>(3L, list.iterator());
        assertThat(i.getSize()).contains(3L);
        assertThat(i.getTotalSize()).contains(3L);
        test(i);
    }

    @Test
    public void wrapWithAtomicLong() {
        BasicWrappedIterator<String> i = new BasicWrappedIterator<>(new AtomicLong(3), list.iterator());
        assertThat(i.getSize()).contains(3L);
        assertThat(i.getTotalSize()).contains(3L);
        test(i);
    }

    protected void test(BasicWrappedIterator<String> i) {
        assertThat(i.getCount()).isEqualTo(0);
        assertThat(i.next()).isEqualTo("a");
        assertThat(i.getCount()).isEqualTo(1);
        assertThat(i.next()).isEqualTo("b");
        assertThat(i.getCount()).isEqualTo(2);
        assertThat(i.hasNext()).isTrue();
        assertThat(i.next()).isEqualTo("c");
        assertThat(i.getCount()).isEqualTo(3);
        assertThatThrownBy(i::next).isInstanceOf(NoSuchElementException.class);
        assertThat(i.hasNext()).isFalse();
        assertThat(i.getCount()).isEqualTo(3);
    }

}
