package nl.vpro.util;

import java.time.*;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalUnit;
import java.util.function.Supplier;

/**
 * Supplies the 'now', but with a certain granularity.
 * @since 2.26
 */
public class  GranularNowSupplier implements Supplier<Instant> {
    private final Clock clock;
    private final Duration lastLoginGranularity;
    Instant lastLogin;
    Instant nextLastLogin;

    public GranularNowSupplier(Duration lastLoginGranularity) {
        this(Clock.systemDefaultZone(), lastLoginGranularity);
    }

    GranularNowSupplier(Clock clock, Duration lastLoginGranularity) {
        this.clock = clock;
        this.lastLoginGranularity = lastLoginGranularity;
        this.nextLastLogin = clock.instant().truncatedTo(getOrderOfMagnitude(lastLoginGranularity));
        tick();
    }

    static TemporalUnit getOrderOfMagnitude(Duration duration) {
        if (duration.compareTo(Duration.ofDays(1)) > 0) {
            return ChronoUnit.DAYS;
        } else if (duration.compareTo(Duration.ofHours(1)) > 0) {
            return ChronoUnit.HOURS;
        } else if (duration.compareTo(Duration.ofMinutes(1)) > 0) {
            return ChronoUnit.MINUTES;
        } else if (duration.compareTo(Duration.ofSeconds(1)) > 0) {
            return ChronoUnit.MINUTES;
        } else {
            return ChronoUnit.MILLIS;
        }
    }

    protected void tick() {
        lastLogin = nextLastLogin;
        nextLastLogin = lastLogin.plus(lastLoginGranularity);
    }

    @Override
    public Instant get() {
        Instant now = clock.instant();
        while (now.isAfter(nextLastLogin)) {
            tick();
        }
        return lastLogin;
    }
}
