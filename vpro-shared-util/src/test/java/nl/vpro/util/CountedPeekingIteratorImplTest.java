package nl.vpro.util;

import java.util.Arrays;
import java.util.NoSuchElementException;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * @author Michiel Meeuwissen
 */
class CountedPeekingIteratorImplTest {

    @Test
    public void basic() throws Exception {
        BasicWrappedIterator<String> wrapped = BasicWrappedIterator.<String>builder()
            .wrapped(Arrays.asList("a", "b", "c").iterator())
            .size(3L)
            .build();
        try(CountedPeekingIterator<String> i = wrapped.peeking()) {

            assertThat(i.peek()).isEqualTo("a");
            assertThat(i.peek()).isEqualTo("a");
            assertThat(i.next()).isEqualTo("a");
            assertThat(i.peek()).isEqualTo("b");
            assertThat(i.peek()).isEqualTo("b");
            assertThat(i.getCount()).isEqualTo(2L);
            assertThat(i.next()).isEqualTo("b");
            assertThat(i.hasNext()).isTrue();
            assertThat(i.next()).isEqualTo("c");
            assertThatThrownBy(i::peek).isInstanceOf(NoSuchElementException.class);
            assertThat(i.hasNext()).isFalse();

            assertThat(i.peeking()).isSameAs(i);
        }



    }

}
