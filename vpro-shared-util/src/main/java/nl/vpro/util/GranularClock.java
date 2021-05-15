package nl.vpro.util;

import lombok.Getter;

import java.time.*;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalUnit;

import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * A clock with limited granularity. It wraps another {@link Clock}, but it will only return (via {@link #instant()}) values
 * that are multiples of the given {@link Duration} {@link #getGranularity()}.
 *
 * @author Michiel Meeuwissen
 * @since 2.26
 */
public class GranularClock extends Clock  {

    @Getter
    private final Clock wrapped;

    @Getter
    private final Duration granularity;

    /**
     * The last return value of {@link #instant()}. This will be used also as a starting point of the next value to return.
     */
    private Instant granularValue;

    public static GranularClock of(Duration granularity) {
        return GranularClock.builder().granularity(granularity).build();
    }

    /**
     * @param clock The clock on which this clock is based. Defaults to {@link Clock#systemDefaultZone()}
     * @param granularity The granularity of this clock. {@link #instant()} will return the initial value offset with an integer multiple of this.
     * @param initialValue The value from which returned values will be calculated. Defaults to a truncated (using {@link GranularClock#getOrderOfMagnitude(Duration)}) value of {@link Clock#instant()}. Another sensible value which you might want to supply could e.g. be {@link Instant#EPOCH}
     */
    @lombok.Builder
    GranularClock(@Nullable Clock clock, @NonNull Duration granularity, @Nullable Instant initialValue) {
        this.wrapped = clock == null ? Clock.systemDefaultZone() : clock;
        this.granularity = granularity;
        this.granularValue = initialValue == null ? this.wrapped.instant().truncatedTo(getOrderOfMagnitude(granularity)) : initialValue;
    }

    @Override
    public ZoneId getZone() {
        return wrapped.getZone();
    }

    @Override
    public Clock withZone(ZoneId zone) {
        return GranularClock.builder()
            .clock(wrapped.withZone(zone))
            .granularity(granularity)
            .initialValue(granularValue)
            .build();
    }

    @Override
    public Instant instant() {
        final Instant now = wrapped.instant();
        final long diff = Duration.between(granularValue, now).toMillis() / granularity.toMillis();
        if (diff != 0) {
            granularValue = granularValue.plus(granularity.multipliedBy(diff));
        }
        return granularValue;
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

}
