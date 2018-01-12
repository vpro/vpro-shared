package nl.vpro.util;

import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

import org.junit.Test;

import com.google.common.collect.Lists;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Michiel Meeuwissen
 * @since 0.32
 */
public class MergedSortedIteratorTest {

    @Test
    public void test() {
        List<String> l1 = Arrays.asList("a", "d");
        List<String> l2 = Arrays.asList("b", "c", "e");

        CountedIterator<String> merged = MergedSortedIterator.merge(Comparator.naturalOrder(), CountedIterator.of(l1), CountedIterator.of(l2));

        assertThat(Lists.newArrayList(merged)).isEqualTo(Arrays.asList("a", "b", "c", "d", "e"));
        assertThat(merged.getSize().get()).isEqualTo(5L);
        assertThat(merged.getTotalSize().get()).isEqualTo(5L);


    }

    @Test
    public void testInSameThread() {
        List<String> l1 = Arrays.asList("a", "d");
        List<String> l2 = Arrays.asList("b", "c", "e");

        CountedIterator<String> merged = MergedSortedIterator.mergeInSameThread(Comparator.naturalOrder(), CountedIterator.of(l1), CountedIterator.of(l2));

        assertThat(Lists.newArrayList(merged)).isEqualTo(Arrays.asList("a", "b", "c", "d", "e"));
        assertThat(merged.getSize().get()).isEqualTo(5L);
        assertThat(merged.getTotalSize().get()).isEqualTo(5L);


    }

    @Test
    public void testInSameThread2() {
        List<String> l1 = Arrays.asList("a", "d");
        List<String> l2 = Arrays.asList("b", "c", "e");

        CountedIterator<String> merged = MergedSortedIterator.mergeInSameThread(Comparator.naturalOrder(), CountedIterator.of(l2), CountedIterator.of(l1));

        assertThat(Lists.newArrayList(merged)).isEqualTo(Arrays.asList("a", "b", "c", "d", "e"));
        assertThat(merged.getSize().get()).isEqualTo(5L);
        assertThat(merged.getTotalSize().get()).isEqualTo(5L);

    }

}
