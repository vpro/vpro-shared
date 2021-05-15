package nl.vpro.util;

import java.time.*;
import java.time.temporal.ChronoUnit;

import org.junit.jupiter.api.Test;
import org.meeuw.math.TestClock;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

class GranularClockTest {

    @Test
    public void tick() {
        TestClock clock = new TestClock(ZoneId.of("Europe/Amsterdam"), Instant.parse("2021-05-14T15:20:14.1234Z"));
        GranularClock granularClock = new GranularClock(clock, Duration.ofMinutes(5), null);

        assertThat(granularClock.instant()).isEqualTo("2021-05-14T15:20:00Z");
        clock.tick(Duration.ofMinutes(4));
        assertThat(granularClock.instant()).isEqualTo("2021-05-14T15:20:00Z");
        clock.tick(Duration.ofMinutes(1));
        assertThat(granularClock.instant()).isEqualTo("2021-05-14T15:25:00Z");

        clock.tick(Duration.ofDays(500));
        assertThat(granularClock.instant()).isEqualTo("2022-09-26T15:25:00Z");

        clock.tick(Duration.ofDays(-1000));
        assertThat(granularClock.instant()).isEqualTo("2019-12-31T15:30:00Z");

    }

    @Test
    public void order() {
        TestClock clock = new TestClock(ZoneId.of("Europe/Amsterdam"), Instant.parse("2021-05-14T15:20:14.1234Z"));

        {
            GranularClock granularClock = new GranularClock(clock, Duration.ofMillis(5), null);
            assertThat(granularClock.instant()).isEqualTo("2021-05-14T15:20:14.123Z");
        }
        {
            GranularClock granularClock = new GranularClock(clock, Duration.ofSeconds(5), null);
            assertThat(granularClock.instant()).isEqualTo("2021-05-14T15:20:10Z");
        }
        {
            GranularClock granularClock = new GranularClock(clock, Duration.ofHours(4), null);
            assertThat(granularClock.instant()).isEqualTo("2021-05-14T15:00:00Z");
        }

        {
            GranularClock granularClock = new GranularClock(clock, Duration.ofDays(4), null);
            assertThat(granularClock.instant()).isEqualTo("2021-05-14T00:00:00Z");
        }
    }

    @Test
    public void build() {
        GranularClock clock = GranularClock.builder()
            .granularity(Duration.ofMinutes(1))
            .initialValue(Instant.EPOCH)
            .build();
        assertThat(clock.getZone()).isEqualTo(ZoneId.systemDefault());
        assertThat(clock.withZone(ZoneId.of("Asia/Kolkata"))).isInstanceOf(GranularClock.class);

    }

    @Test
    public void of() {
        GranularClock clock = GranularClock.of(Duration.ofMinutes(1));
        assertThat(clock.instant()).isCloseTo(Instant.now(), within(2, ChronoUnit.MINUTES));

    }

}
