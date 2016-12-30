package nl.vpro.util;

import java.util.*;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

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
}
