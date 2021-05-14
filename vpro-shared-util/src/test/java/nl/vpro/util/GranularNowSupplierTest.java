package nl.vpro.util;

import java.time.*;

import org.junit.jupiter.api.Test;
import org.meeuw.math.TestClock;

import static org.assertj.core.api.Assertions.assertThat;

class GranularNowSupplierTest {

    @Test
    public void tick() {
        TestClock clock = new TestClock(ZoneId.of("Europe/Amsterdam"), Instant.parse("2021-05-14T15:20:14.1234Z"));
        GranularNowSupplier granularNowSupplier = new GranularNowSupplier(clock, Duration.ofMinutes(5));

        assertThat(granularNowSupplier.get()).isEqualTo("2021-05-14T15:20:00Z");
        clock.tick(Duration.ofMinutes(4));
        assertThat(granularNowSupplier.get()).isEqualTo("2021-05-14T15:20:00Z");
        clock.tick(Duration.ofMinutes(1));
        assertThat(granularNowSupplier.get()).isEqualTo("2021-05-14T15:25:00Z");
    }

    @Test
    public void order() {
        TestClock clock = new TestClock(ZoneId.of("Europe/Amsterdam"), Instant.parse("2021-05-14T15:20:14.1234Z"));

        {
            GranularNowSupplier granularNowSupplier = new GranularNowSupplier(clock, Duration.ofMillis(5));
            assertThat(granularNowSupplier.get()).isEqualTo("2021-05-14T15:20:14.123Z");
        }
        {
            GranularNowSupplier granularNowSupplier = new GranularNowSupplier(clock, Duration.ofSeconds(5));
            assertThat(granularNowSupplier.get()).isEqualTo("2021-05-14T15:20:10Z");
        }
        {
            GranularNowSupplier granularNowSupplier = new GranularNowSupplier(clock, Duration.ofHours(4));
            assertThat(granularNowSupplier.get()).isEqualTo("2021-05-14T15:00:00Z");
        }

        {
            GranularNowSupplier granularNowSupplier = new GranularNowSupplier(clock, Duration.ofDays(4));
            assertThat(granularNowSupplier.get()).isEqualTo("2021-05-14T00:00:00Z");
        }
    }

}
