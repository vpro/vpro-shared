package nl.vpro.util;

import java.util.SortedSet;
import java.util.TreeSet;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Michiel Meeuwissen
 * @since 1.63
 */
class TransformingSortedSetTest {


    @Test
    void add() {
        SortedSet<Integer> set = new TreeSet<>();
        set.add(1);
        set.add(2);

        TransformingSortedSet<StringBuilder, Integer> transforming =
            new TransformingSortedSet<>(set, i -> new StringBuilder(String.valueOf(i)), stringBuilder -> Integer.parseInt(stringBuilder.toString()));
        transforming.add(new StringBuilder("3"));

        assertThat(set).containsExactly(1, 2, 3);
    }


    @Test
    void addToEmpty() {
        SortedSet<Integer> set = new TreeSet<>();
        assertThat(set.comparator()).isNull();
        TransformingSortedSet<StringBuilder, Integer> transforming =
            new TransformingSortedSet<>(set, i -> new StringBuilder(String.valueOf(i)), stringBuilder -> Integer.parseInt(stringBuilder.toString()));
        transforming.add(new StringBuilder("3"));


        assertThat(set).containsExactly(3);
    }


    @Test
    void remove() {
        SortedSet<Integer> set = new TreeSet<>();
        set.add(1);
        set.add(2);

        TransformingSortedSet<StringBuilder, Integer> transforming =
            new TransformingSortedSet<>(set, i -> new StringBuilder(String.valueOf(i)), stringBuilder -> Integer.parseInt(stringBuilder.toString()));
        transforming.remove(new StringBuilder("2"));

        assertThat(set).containsExactly(1);

    }

    @Test
    void changeFirst() {
        SortedSet<Integer> set = new TreeSet<>();
        set.add(1);
        set.add(2);

        TransformingSortedSet<StringBuilder, Integer> transforming =
            new TransformingSortedSet<>(set, i -> new StringBuilder(String.valueOf(i)), stringBuilder -> Integer.parseInt(stringBuilder.toString()));
        transforming.first().append("0");
        assertThat(transforming.first().toString()).isEqualTo("10");
        set = transforming.produce();

        assertThat(set).containsExactly(2, 10);

    }

    @Test
    void comparator() {
        SortedSet<Integer> set = new TreeSet<>();

        TransformingSortedSet<StringBuilder, Integer> transforming =
            new TransformingSortedSet<>(set, i -> new StringBuilder(String.valueOf(i)), stringBuilder -> Integer.parseInt(stringBuilder.toString()));

        assertThat(transforming.comparator().compare(new StringBuilder("01"), new StringBuilder("2"))).isLessThan(0);

    }
}
