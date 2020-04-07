package nl.vpro.util;


import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Michiel Meeuwissen
 * @since ...
 */
public class TriPredicateTest {
    static class My implements TriPredicate<String, Integer, Float> {
        @Override
        public boolean test(String s, Integer integer, Float aFloat) {
            float  f = Float.parseFloat(s);
            return f > aFloat && f > integer;
        }
    }
    static class You implements TriPredicate<String, Integer, Float> {
        @Override
        public boolean test(String s, Integer integer, Float aFloat) {
            float  f = Float.parseFloat(s);
            return f >= aFloat && f >= integer;
        }
    }


    My my = new My();
    You you = new You();

    @Test
    public void test1() {
        assertThat(my.test("123", 120, 120f)).isTrue();
        assertThat(my.test("1", 120, 120f)).isFalse();
    }

    @Test
    public void and() {
        assertThat(my.and(you).test("123", 120, 120f)).isTrue();
        assertThat(my.and(you).test("120", 120, 120f)).isFalse();
    }

    @Test
    public void negate() {
        assertThat(my.negate().test("123", 120, 120f)).isFalse();
        assertThat(my.negate().test("1", 120, 120f)).isTrue();
    }

    @Test
    public void or() {
        assertThat(my.or(you).test("123", 120, 120f)).isTrue();
        assertThat(my.or(you).test("120", 120, 120f)).isTrue();
        assertThat(my.or(you).test("100", 120, 120f)).isFalse();

    }
}
