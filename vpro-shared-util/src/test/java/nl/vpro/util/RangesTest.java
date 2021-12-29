package nl.vpro.util;

import org.junit.jupiter.api.Test;

import static nl.vpro.util.Ranges.closedOpen;
import static nl.vpro.util.Ranges.convert;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RangesTest {

    static class A extends Number implements Comparable<Number> {

        private final long longValue;

        A(long longValue) {
            this.longValue = longValue;
        }

        @Override
        public int compareTo(Number o) {
            return intValue() - o.intValue();
        }

        @Override
        public int intValue() {
            return (int) longValue;
        }

        @Override
        public long longValue() {
            return longValue;
        }

        @Override
        public float floatValue() {
            return (float) longValue;
        }

        @Override
        public double doubleValue() {
            return (double) longValue;
        }
        @Override
        public String toString() {
            return String.valueOf(longValue);
        }

        public A neg() {
            return new A(-1 * longValue);
        }
    }

    @Test
    void closedOpenAndConvert() {
        assertThatThrownBy(() ->
            closedOpen(2, 1)
        ).isInstanceOf(IllegalArgumentException.class);

        assertThat(convert(closedOpen(1, 2), i -> i + 1).toString()).isEqualTo("[2..3)");
        assertThat(convert(closedOpen(1, null), i -> i * 3).toString()).isEqualTo("[3..+∞)");

        assertThat(convert(closedOpen(null, new A(2)), A::neg).toString()).isEqualTo("(-∞..-2)"); // TODO: we would probably expect (-2..+∞), but that would be hard to do genericly.

        // this would work:
        assertThat(convert(closedOpen(Double.NEGATIVE_INFINITY, 2.0d), d -> d * -1).toString()).isEqualTo("(-2.0..Infinity]");
        assertThat(convert(closedOpen(new A(1), new A(2)), A::neg).toString()).isEqualTo("(-2..-1]");
        assertThat(convert(closedOpen((Integer) null, null), i -> i).toString()).isEqualTo("(-∞..+∞)");
    }


}
