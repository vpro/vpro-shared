package nl.vpro.util;

import java.util.*;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * @author Michiel Meeuwissen
 * @since 2.23
 */
class CountedMaxOffsetIteratorTest {

    @Test
    public void test() {
        List<String> list = Arrays.asList("a", "b", "c", "d", "e");
        CountedMaxOffsetIterator<String> i = MaxOffsetIterator
            .<String>countedBuilder()
            .wrapped(CountedIterator.of(list))
            .offset(1)
            .max(2)
            .build();

        assertThat(i.getCount()).isEqualTo(-1);
        assertThat(i.next()).isEqualTo("b");
        assertThat(i.getSize()).contains(2L);
        assertThat(i.getTotalSize()).contains(5L);
        assertThat(i.next()).isEqualTo("c");
        assertThatThrownBy(i::next).isInstanceOf(NoSuchElementException.class);

        assertThat(i.toString()).matches("Counted\\[Closeable\\[.*]\\[1,2]");
    }

}
