package nl.vpro.util;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.Test;

import com.google.common.base.Predicate;

import static org.fest.assertions.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

/**
 * @author Michiel Meeuwissen
 * @since 1.8
 */
public class FilteringIteratorTest {

    private static final Predicate<String> notC = new Predicate<String>() {
        @Override
        public boolean apply(String input) {
            return !"c".equals(input);
        }
    };

    @Test
    public void test() {
        List<String> list = Arrays.asList("a", "b", "c", null, "d");
        AtomicInteger i = new AtomicInteger(0);
        Iterator<String> iterator = new FilteringIterator<>(list.iterator(), notC, FilteringIterator.KeepAlive.of(2, c ->
        {i.incrementAndGet();}));
        StringBuilder build = new StringBuilder();
        while(iterator.hasNext()) {
            iterator.hasNext(); // check that you can call it multiple times
            build.append(iterator.next());
        }
        assertEquals("abnulld", build.toString());
        assertThat(i.get()).isEqualTo(2);

    }

    @Test
    public void testWithNull() {
        List<String> list = Arrays.asList("a", "b", "c", null, "d");

        Iterator<String> iterator = new FilteringIterator<>(list.iterator(), null);
        StringBuilder build = new StringBuilder();
        while (iterator.hasNext()) {
            build.append(iterator.next());
        }
        assertEquals("abcnulld", build.toString());

    }

    @Test(expected = NoSuchElementException.class)
    public void noSuchElement() {
        Iterator<String> iterator = new FilteringIterator<>(Arrays.asList("a", "b", "c", null, "d").iterator(), notC);
        assertEquals("a", iterator.next());
        assertEquals("b", iterator.next());
        assertNull(iterator.next());
        assertEquals("d", iterator.next());

        iterator.next();
    }


    @Test
    public void testRemove() {
        List<String> list = new ArrayList<>(Arrays.asList("a", "b", "c", null, "d"));

        Iterator<String> iterator = new FilteringIterator<>(list.iterator(), null);
        while (iterator.hasNext()) {
            if ("b".equals(iterator.next())) {
                iterator.remove();
            }
        }
        assertEquals(Arrays.asList("a", "c", null, "d"), list);

    }

    @Test
    public void testRemove2() {
        List<String> list = new ArrayList<>(Arrays.asList("a", "b", "c", null, "d"));

        Iterator<String> iterator = new FilteringIterator<>(list.iterator(), new Predicate<String>() {
            @Override
            public boolean apply(String input) {
                return input == null || input.equals("b");
            }
        });
        while (iterator.hasNext()) {
            iterator.next();
            iterator.remove();
        }
        assertEquals(Arrays.asList("a", "c", "d"), list);

    }

}
