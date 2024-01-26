package nl.vpro.util;

import java.time.Duration;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ComparableUtilsTest {

    @Test
    public void max() {
        assertThat(ComparableUtils.max(1, 2, 3, null, -1)).isEqualTo(3);
    }

    @Test
    public void min() {
        assertThat(ComparableUtils.min(Duration.ofMillis(-10), Duration.ZERO, null, Duration.ofMinutes(10))).isEqualTo(Duration.ofMillis(-10));
    }


    @Test
    public void coalesce() {
        assertThat(ComparableUtils.coalesce(Duration.ofMillis(-10), Duration.ZERO, null, Duration.ofMinutes(10))).isEqualTo(Duration.ofMillis(-10));

        assertThat((Object) ComparableUtils.coalesce(null, null, null)).isNull();

    }

}
