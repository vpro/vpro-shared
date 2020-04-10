package nl.vpro.util;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;


/**
 * @author Michiel Meeuwissen
 * @since 2.18
 */
public class PredicatesTest {

    @Test
    public void alwaysFalse() {
        assertThat(Predicates.<String>alwaysFalse().test("a")).isFalse();
        assertThat(Predicates.<String>alwaysFalse().toString()).isEqualTo("FALSE");
    }

    @Test
    public void alwaysTrue() {
        assertThat(Predicates.<String>alwaysTrue().test("a")).isTrue();
        assertThat(Predicates.<String>alwaysTrue().toString()).isEqualTo("TRUE");
    }

    @Test
    public void biAlwaysFalse() {
        assertThat(Predicates.biAlwaysFalse().test("a", "b")).isFalse();
        assertThat(Predicates.biAlwaysFalse().toString()).isEqualTo("FALSE");
    }

    @Test
    public void biAlwaysTrue() {
        assertThat(Predicates.<String, Integer>biAlwaysTrue().test("a", 2)).isTrue();
        assertThat(Predicates.<String, Integer>biAlwaysTrue().toString()).isEqualTo("TRUE");
    }

    @Test
    public void triAlwaysFalse() {
        assertThat(Predicates.<String, Integer, Float>triAlwaysFalse().test("a", 2, 3.0f)).isFalse();
        assertThat(Predicates.<String, Integer, Float>triAlwaysFalse().toString()).isEqualTo("FALSE");
    }

    @Test
    public void triAlwaysTrue() {
        assertThat(Predicates.<String, Integer, Float>triAlwaysTrue().test("a", 2, 3.0f)).isTrue();
        assertThat(Predicates.<String, Integer, Float>triAlwaysTrue().toString()).isEqualTo("TRUE");
    }
}
