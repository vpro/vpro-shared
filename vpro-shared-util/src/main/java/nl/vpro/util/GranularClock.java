package nl.vpro.util;

import lombok.Getter;

import java.time.*;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalUnit;

import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * A clock with limited granularity. It wraps another {@link Clock}, but it will only return (vi {@link #instant()}) values
 * that are multiples of the given {@link Duration} {@link #getGranularity()}.
 *
 * @since 2.26
 */
public class GranularClock extends Clock {
    private final Clock clock;

    @Getter
    private final Duration granularity;

    Instant granularValue;

    public static GranularClock of(Duration granularity) {
        return GranularClock.builder().granularity(granularity).build();
    }

    @lombok.Builder
    GranularClock(@Nullable Clock clock, @NonNull Duration granularity, @Nullable Instant initialValue) {
        this.clock = clock == null ? Clock.systemDefaultZone() : clock;
        this.granularity = granularity;
        this.granularValue = initialValue == null ? this.clock.instant().truncatedTo(getOrderOfMagnitude(granularity)) : initialValue;
    }

    @Override
    public ZoneId getZone() {
        return clock.getZone();
    }

    @Override
    public Clock withZone(ZoneId zone) {
        return GranularClock.builder()
            .clock(clock.withZone(zone))
            .granularity(granularity)
            .initialValue(granularValue)
            .build();
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


    @Override
    public Instant instant() {
        Instant now = clock.instant();
        long diff = Duration.between(granularValue, now).toMillis() / granularity.toMillis();
        granularValue = granularValue.plus(granularity.multipliedBy(diff));
        return granularValue;
    }
}
