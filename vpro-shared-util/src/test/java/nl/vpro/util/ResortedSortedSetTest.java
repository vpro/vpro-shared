package nl.vpro.util;

import java.util.*;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

/**
 * @author Michiel Meeuwissen
 * @since 2.1
 */
public class ResortedSortedSetTest {


    @Test
    public void test() {

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
    public void testAdd() {
        Collection<String> test = new ArrayList<>();
        test.addAll(Arrays.asList("b", "a"));

        SortedSet<String> resorted = new ResortedSortedSet<>(test, String::compareTo);

        assertThat(resorted.contains("a")).isTrue();
        assertThat(resorted.add("a")).isFalse();
        assertThat(resorted).containsExactly("a", "b");

    }

    @Test
    public void testRemove() {
        Collection<String> test = new ArrayList<>();
        test.addAll(Arrays.asList("b", "a"));

        SortedSet<String> resorted = new ResortedSortedSet<>(test, String::compareTo);

        assertThat(resorted.contains("a")).isTrue();
        assertThat(resorted.remove("a")).isTrue();
        assertThat(resorted).containsExactly("b");

    }

    @Test
    public void testIteratorRemove() {
        Collection<String> test = new ArrayList<>();
        test.addAll(Arrays.asList("b", "a"));

        SortedSet<String> resorted = new ResortedSortedSet<>(test, String::compareTo);

        Iterator<String> i = resorted.iterator();
        String a = i.next();
        assertThat(a).isEqualTo("a");
        i.remove();

        assertThat(resorted).containsExactly("b");

        try {
            i.remove();
            fail("Should have thrown IllegalStateEcxception");
        } catch (IllegalStateException ignore) {

        }
        String b = i.next();
        assertThat(b).isEqualTo("b");
        i.remove();
        assertThat(resorted).isEmpty();
        assertThat(i.hasNext()).isFalse();

        try {
            i.next();
            fail("Should have thrown NoSuchElementException");
        } catch (NoSuchElementException ignore) {

        }

    }
}
