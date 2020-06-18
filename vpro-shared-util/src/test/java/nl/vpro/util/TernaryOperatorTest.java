package nl.vpro.util;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Michiel Meeuwissen
 * @since 2.12
 */
class TernaryOperatorTest {

    @Test
    void minBy() {
        assertThat(TernaryOperator.minBy(String::compareTo).apply("a", "b", "c")).isEqualTo("a");
        assertThat(TernaryOperator.minBy(String::compareTo).apply("a", "c", "b")).isEqualTo("a");
        assertThat(TernaryOperator.minBy(String::compareTo).apply("b", "a", "c")).isEqualTo("a");
        assertThat(TernaryOperator.minBy(String::compareTo).apply("b", "c", "a")).isEqualTo("a");
        assertThat(TernaryOperator.minBy(String::compareTo).apply("c", "a", "b")).isEqualTo("a");
        assertThat(TernaryOperator.minBy(String::compareTo).apply("c", "b", "a")).isEqualTo("a");

        assertThat(TernaryOperator.minBy(String::compareTo).apply("a", "a", "b")).isEqualTo("a");
        assertThat(TernaryOperator.minBy(String::compareTo).apply("a", "b", "a")).isEqualTo("a");
        assertThat(TernaryOperator.minBy(String::compareTo).apply("b", "a", "a")).isEqualTo("a");


    }

    @Test
    void maxBy() {
        assertThat(TernaryOperator.maxBy(String::compareTo).apply("a", "b", "c")).isEqualTo("c");
        assertThat(TernaryOperator.maxBy(String::compareTo).apply("a", "c", "b")).isEqualTo("c");
        assertThat(TernaryOperator.maxBy(String::compareTo).apply("b", "a", "c")).isEqualTo("c");
        assertThat(TernaryOperator.maxBy(String::compareTo).apply("b", "c", "a")).isEqualTo("c");
        assertThat(TernaryOperator.maxBy(String::compareTo).apply("c", "a", "b")).isEqualTo("c");
        assertThat(TernaryOperator.maxBy(String::compareTo).apply("c", "b", "a")).isEqualTo("c");

    }
}
