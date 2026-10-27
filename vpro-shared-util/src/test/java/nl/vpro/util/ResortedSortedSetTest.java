package nl.vpro.util;

import java.util.*;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * @author Michiel Meeuwissen
 * @since 2.1
 */
class ResortedSortedSetTest {


    @Test
    void test() {

        Collection<String> test = new ArrayList<>();
        test.addAll(Arrays.asList("b", "a"));

        SortedSet<String> resorted = new ResortedSortedSet<>(test, String::compareTo);
        {
            Iterator<String> i = resorted.iterator();
            assertThat(i.next()).isEqualTo("a");
            assertThat(i.next()).isEqualTo("b");
        }

        {
            Iterator<String> i = resorted.iterator();
            assertThat(i.next()).isEqualTo("a");
            i.remove();
            assertThat(i.next()).isEqualTo("b");
            assertThat(test).hasSize(1);

        }


    }


    @Test
    void add() {
        Collection<String> test = new ArrayList<>();
        test.addAll(Arrays.asList("b", "a"));

        SortedSet<String> resorted = new ResortedSortedSet<>(test, String::compareTo);

        assertThat(resorted.contains("a")).isTrue();
        assertThat(resorted.add("a")).isFalse();
        assertThat(resorted).containsExactly("a", "b");

    }

    @Test
    void testRemove() {
        Collection<String> test = new ArrayList<>();
        test.addAll(Arrays.asList("b", "a"));

        SortedSet<String> resorted = new ResortedSortedSet<>(test, String::compareTo);

        assertThat(resorted.contains("a")).isTrue();
        assertThat(resorted.remove("a")).isTrue();
        assertThat(resorted).containsExactly("b");

    }

    @Test
    void iteratorRemove() {
        Collection<String> test = new ArrayList<>();
        test.addAll(Arrays.asList("b", "a"));

        SortedSet<String> resorted = new ResortedSortedSet<>(test, String::compareTo);

        Iterator<String> i = resorted.iterator();
        String a = i.next();
        assertThat(a).isEqualTo("a");
        i.remove();

        assertThat(resorted).containsExactly("b");

        assertThatThrownBy(i::remove).isInstanceOf(IllegalStateException.class);

        String b = i.next();
        assertThat(b).isEqualTo("b");
        i.remove();
        assertThat(resorted).isEmpty();
        assertThat(i.hasNext()).isFalse();

        assertThatThrownBy(i::next).isInstanceOf(NoSuchElementException.class);

    }
}
