package nl.vpro.util;

import java.util.*;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;

/**
 * @author Michiel Meeuwissen
 * @since 1.68
 */
public class SkippingIteratorTest {

    @Test
    public void basicTest() {
        List<String> test = Arrays.asList("a", "b", "b", "c");

        SkippingIterator<String> iterator = new SkippingIterator<>(test.iterator());
        assertThat(iterator).toIterable().containsExactly("a", "b", "c");

    }


    @Test
    public void basicTest2() {
        List<String> test = Arrays.asList("a", "b", "x", "b", "c", null, null, "a");

        SkippingIterator<String> iterator = new SkippingIterator<>(test.iterator(),
            Objects::equals);
        assertThat(iterator).toIterable().containsExactly("a", "b", "x", "b", "c", null, "a");

    }


    @Test
    public void basicTest3() {
        List<String> test = Arrays.asList("a", "b", "x", "x", "b", "b", "c", null, null, "a");

        SkippingIterator<String> iterator = new SkippingIterator<>(test.iterator(), (a, b) ->
            Objects.equals(a, b) && "x".equals(a));
        assertThat(iterator).toIterable().containsExactly("a", "b", "x", "b", "b", "c", null, null, "a");

    }


    @Test
    public void basicTest4() {
        List<String> test = Arrays.asList("a", "b", "b", "b", "c");

        SkippingIterator<String> iterator = new SkippingIterator<>(test.iterator());
        assertThat(iterator).toIterable().containsExactly("a", "b", "c");

    }
}
