package nl.vpro.util;

import java.util.Arrays;
import java.util.List;

import org.junit.Test;

import com.google.common.collect.Lists;

import static org.assertj.core.api.Assertions.assertThat;

public class MaxOffsetIteratorTest {

    @Test
    public void testMax() {
        List<String> test = Arrays.asList("a", "b", "c", "d");
        assertThat(Lists.newArrayList(new MaxOffsetIterator<>(test.iterator(), 2))).containsExactly("a", "b");
    }

    @Test
    public void testMaxNull() {
        List<String> test = Arrays.asList("a", "b", "c", "d");
        assertThat(Lists.newArrayList(new MaxOffsetIterator<>(test.iterator(), null))).containsExactly("a", "b", "c", "d");
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


}
