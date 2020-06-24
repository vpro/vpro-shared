package nl.vpro.util;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Michiel Meeuwissen
 * @since 2.12
 */
class QuaternaryOperatorTest {

    @Test
    void minBy() {
        assertThat(QuaternaryOperator.minBy(String::compareTo).apply("a", "b", "c", "d")).isEqualTo("a");
        assertThat(QuaternaryOperator.minBy(String::compareTo).apply("a", "c", "b", "d")).isEqualTo("a");
        assertThat(QuaternaryOperator.minBy(String::compareTo).apply("b", "a", "c", "d")).isEqualTo("a");
        assertThat(QuaternaryOperator.minBy(String::compareTo).apply("b", "c", "a", "d")).isEqualTo("a");
        assertThat(QuaternaryOperator.minBy(String::compareTo).apply("c", "a", "b", "d")).isEqualTo("a");
        assertThat(QuaternaryOperator.minBy(String::compareTo).apply("c", "b", "a", "d")).isEqualTo("a");

    }

}
