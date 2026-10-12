package nl.vpro.util;

import lombok.extern.slf4j.Slf4j;

import java.time.*;
import java.time.temporal.*;
import java.util.Date;
import java.util.Optional;

import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.checker.nullness.qual.PolyNull;

/**
 * @author Michiel Meeuwissen
 * @since 0.45
 */
@Slf4j
public class TimeUtils {

    public static final ZoneId ZONE_ID =  ZoneId.of("Europe/Amsterdam");
    public static final ZonedDateTime LOCAL_EPOCH = Instant.EPOCH.atZone(ZONE_ID);

    public static final Duration MAX_DURATION = Duration.ofSeconds(Long.MAX_VALUE, 999_999_999);


    @Deprecated
    public static Optional<ZonedDateTime> parseZoned(CharSequence parse) {
        return org.meeuw.time.TimeUtils.parseZoned(parse);

    }
    @Deprecated
    public static Optional<Instant> parse(CharSequence dateValue) {
        return org.meeuw.time.TimeUtils.parse(dateValue);
    }

    @Deprecated
    public static Optional<Duration> parseDuration(CharSequence d) {
        return org.meeuw.time.TimeUtils.parseDuration( d);
    }


    /**
     * @since 5.0
     */
    @PolyNull
    @Deprecated
    public static Duration parseDurationOrThrow(@PolyNull CharSequence d) {
        return org.meeuw.time.TimeUtils.parseDurationOrThrow( d);
    }

    @Deprecated
    public static Optional<Duration> parseDuration(CharSequence d, ZonedDateTime at) {
        return org.meeuw.time.TimeUtils.parseDuration( d, at);
    }


    @Deprecated
    public static Optional<? extends TemporalAmount> parseTemporalAmount(@Nullable CharSequence d) {
        return org.meeuw.time.TimeUtils.parseTemporalAmount( d);

    }

    @Deprecated
    public static String toParsableString(Duration duration) {
        return org.meeuw.time.TimeUtils.toParsableString(duration);
    }
    /**
     * @since 2.6
     */
    @Deprecated
    public static Optional<LocalDateTime> parseLocalDateTime(CharSequence d) {
        return org.meeuw.time.TimeUtils.parseLocalDateTime(d);
    }

    /**
     * @since 4.0
     */
    @Deprecated
    public static Optional<LocalDate> parseLocalDate(CharSequence d) {
        return org.meeuw.time.TimeUtils.parseLocalDate(d);
    }

    @Deprecated
    public static Optional<Duration> durationOf(Integer i) {
        return org.meeuw.time.TimeUtils.durationOf(i);
    }

    @Deprecated
    public static Optional<Duration> durationOf(Date i) {
        return Optional.ofNullable(i == null ? null : Duration.ofMillis(i.getTime()));
    }

    @PolyNull
    public static Duration durationOf(javax.xml.datatype.@PolyNull Duration d) {
        return d == null ? null : Duration.parse(d.toString());
    }

    @Deprecated
    public static Optional<Integer> toSecondsInteger(Duration d) {
        return Optional.ofNullable(d == null ? null : (int) (d.toMillis() / 1000));
    }

    @Deprecated
    public static Optional<Float> toSeconds(@Nullable Duration d) {
        return org.meeuw.time.TimeUtils.toSeconds(d);
    }

    @Deprecated
    public static Optional<Long> toMillis(@Nullable Duration d) {
        return Optional.ofNullable(d == null ? null : d.toMillis());
    }

    @Deprecated
    @PolyNull
    public static Date asDate(@PolyNull Duration duration) {
        return duration == null ? null : new Date(duration.toMillis());
    }


    /**
     * {@code null} safe version of {@link Duration#between(Temporal, Temporal)}. If one  or both of the arguments are null, the result is {@code null} too.
     */
    @PolyNull
    @Deprecated
    public static Duration between(@PolyNull Temporal instant1, @PolyNull Temporal instant2) {
        return org.meeuw.time.TimeUtils.between(instant1, instant2);
    }

    @Deprecated
    public static boolean isLarger(@Nullable Duration duration1, @Nullable Duration duration2) {
        return org.meeuw.time.TimeUtils.isLarger(duration1, duration2);
    }

    /**
     * Rounds the duration to the nearest millis (This may round up half a millis).
     */
    @PolyNull
    @Deprecated
    public static Duration roundToMillis(@PolyNull Duration duration) {
        return org.meeuw.time.TimeUtils.roundToMillis(duration);
    }

    /**
     * @since 2.34
     */
    @PolyNull
    @Deprecated
    public static Instant truncatedTo(
        @PolyNull Instant instant,
        @Nullable ChronoUnit unit) {
        return org.meeuw.time.TimeUtils.truncatedTo(instant, unit);
    }

    /**
     * @since 2.34
     */
    @PolyNull
    @Deprecated
    public static Instant truncated(
        @PolyNull Instant instant) {
        return org.meeuw.time.TimeUtils.truncated(instant);
    }


    @Deprecated
    public static LocalTime parseLocalTime(CharSequence t) {
        return org.meeuw.time.TimeUtils.parseLocalTime(t);
    }
}
