package nl.vpro.util;

import java.util.*;

import org.junit.jupiter.api.Test;

import com.google.common.collect.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class MaxOffsetIteratorTest {

    @Test
    public void testMax() {
        List<String> test = Arrays.asList("a", "b", "c", "d");
        assertThat(Lists.newArrayList(new MaxOffsetIterator<>(test.iterator(), 2))).containsExactly("a", "b");
    }

    @Test
    public void testMaxNull() {
        List<String> test = Arrays.asList("a", "b", "c", "d");
        assertThat(Lists.newArrayList(
            new MaxOffsetIterator<>(test.iterator(), null))).containsExactly("a", "b", "c", "d");
    }

    @Test
    public void testMaxOffset() {
        List<String> test = Arrays.asList("a", "b", null, "c", "d");
        assertThat(Lists.newArrayList(new MaxOffsetIterator<>(test.iterator(), 2, 1))).containsExactly("b", null);
    }

    @Test
    public void testMaxOffsetDontcountNulls() {
        List<String> test = Arrays.asList("a", null, "b", "c", null, "d", "e");
        assertThat(Lists.newArrayList(new MaxOffsetIterator<>(test.iterator(), 2, 2, false))).containsExactly("c", null, "d");
    }

    @Test
    public void autoClose() {
        final boolean[] booleans = new boolean[2];
        AutoCloseable autoCloseable = () -> booleans[0] = true;
        Runnable callback  = () -> booleans[1] = true;
        MaxOffsetIterator<String> i = MaxOffsetIterator
            .<String>builder()
            .wrapped(Arrays.asList("a", "b", "c").iterator())
            .max(2)
            .callback(callback)
            .build()
            .autoClose(autoCloseable);

        assertThat(Lists.newArrayList(i)).containsExactly("a", "b");
        assertThat(booleans[0]).isTrue();
        assertThat(booleans[1]).isTrue();
    }


    @Test
    public void testToString() {
        List<String> test = Arrays.asList("a", "b", "c", "d");
        assertThat(new MaxOffsetIterator<>(test.iterator(), 2).toString()).matches("Closeable\\[.*]\\[0,2]");

        assertThat(MaxOffsetIterator.<String>builder().wrapped(test.iterator()).offset(1).build().toString()).matches("Closeable\\[.*]\\[1,]");

    }

    @Test
    public void peeking() {
        List<String> list = Arrays.asList("a", "b", "c", "d");
        PeekingIterator<String> i = Iterators.peekingIterator(list.iterator());
        assertThat(i.peek()).isEqualTo("a");

        MaxOffsetIterator<String> mo = MaxOffsetIterator
            .<String>builder()
            .wrapped(i)
            .max(2)
            .offset(1)
            .build();
        assertThat(mo.peek()).isEqualTo("b");
        assertThat(mo.peekingWrapped().hasNext()).isTrue();

        assertThat(mo.next()).isEqualTo("b");
        assertThat(mo.next()).isEqualTo("c");
        assertThatThrownBy(mo::peek).isInstanceOf(NoSuchElementException.class);
        assertThat(mo.peekingWrapped().peek()).isEqualTo("d");
    }




}
