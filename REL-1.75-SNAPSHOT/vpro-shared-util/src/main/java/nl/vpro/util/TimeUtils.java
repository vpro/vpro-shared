package nl.vpro.util;

import lombok.extern.slf4j.Slf4j;

import java.time.*;
import java.time.format.DateTimeParseException;
import java.util.Date;
import java.util.Optional;

import org.apache.commons.lang3.StringUtils;

/**
 * @author Michiel Meeuwissen
 * @since 0.45
 */
@Slf4j
public class TimeUtils {

    public static ZoneId ZONE_ID = ZoneId.of("Europe/Amsterdam");


    public static Optional<ZonedDateTime> parseZoned(CharSequence parse) {
        if (StringUtils.isBlank(parse)) {
            return Optional.empty();
        }
        try {
            return Optional.of(ZonedDateTime.parse(parse));
        } catch (DateTimeParseException ignored) {

        }
        Instant instant = parse(parse).orElse(null);
        if (instant == null) {
            return Optional.empty();
        }
        return Optional.of(instant.atZone(ZONE_ID));
    }

    public static Optional<Instant> parse(CharSequence dateValue) {
        if (StringUtils.isBlank(dateValue)) {
            return Optional.empty();
        }


        try {
            return Optional.of(LocalDate.parse(dateValue).atStartOfDay().atZone(ZONE_ID).toInstant());
        } catch (DateTimeParseException ignored) {

        }
        try {
            return Optional.of(LocalDateTime.parse(dateValue).atZone(ZONE_ID).toInstant());
        } catch (DateTimeParseException ignored) {

        }
        //return Instant.parse(dateValue);
        try {
            return Optional.of(OffsetDateTime.parse(dateValue).toInstant());
        } catch (DateTimeParseException ignored) {

        }
        try {
            return Optional.of(ZonedDateTime.parse(dateValue).toInstant());
        } catch (DateTimeParseException ignored) {
        }
        DateTimeParseException dtp;
        try {
            return Optional.of(Instant.parse(dateValue));
        } catch (DateTimeParseException e) {
            dtp = e;
        }

        try {
            Long longValue = Long.parseLong(dateValue.toString());
            if (longValue > 1000 && longValue < 9999) {
                return Optional.of(LocalDate.of(longValue.intValue(), 1, 1).atStartOfDay().atZone(ZONE_ID).toInstant());
            } else {
                return Optional.of(Instant.ofEpochMilli(Long.parseLong(dateValue.toString())));
            }
        } catch (NumberFormatException nfe) {
            throw dtp;
        }

    }

    public static Optional<Duration> parseDuration(CharSequence d) {
        if (StringUtils.isBlank(d)) {
            return Optional.empty();
        }
        if (d.toString().startsWith("${")) {// unresolved spring setting;
            log.warn("Found {} as duration, returing empty", d);
            return Optional.empty();
        }


        try {
            return Optional.of(Duration.parse(d));
        } catch (DateTimeParseException dtp) {
            String ds = d.toString();
            try {
                return Optional.of(Duration.ofMillis(Long.parseLong(ds)));
            } catch (NumberFormatException nfe) {
                // ignore
            }
            if (!ds.startsWith("P")) {
                return parseDuration("P" + ds);
            } else if (!ds.startsWith("PT")){
                // so it did start with P, just not with PT, and it couldn't be parsed
                return parseDuration("PT" + ds.substring(1));
            }
            throw new DateTimeParseException(dtp.getParsedString() + ":" + dtp.getMessage(), dtp.getParsedString(), dtp.getErrorIndex());
        }
    }

    public static Optional<Duration> durationOf(Integer i) {
        return Optional.ofNullable(i == null ? null : Duration.ofMillis(i));
    }


    public static Optional<Duration> durationOf(Date i) {
        return Optional.ofNullable(i == null ? null : Duration.ofMillis(i.getTime()));
    }

    public static Duration durationOf(javax.xml.datatype.Duration d) {
        return d == null ? null : Duration.parse(d.toString());
    }

    public static Date asDate(Duration duration) {
        return duration == null ? null : new Date(duration.toMillis());
    }


    public static Duration between(Instant instant1, Instant instant2) {
        if (instant1 == null || instant2 == null) {
            return null;
        }
        return Duration.between(instant1, instant2);
    }

    public static boolean isLarger(Duration duration1, Duration duration2) {
        if (duration1 == null || duration2 == null) {
            return false;
        }
        return duration1.compareTo(duration2) > 0;
    }

    /**
     * Rounds the duration to the nearest millis (This may round up half a millis).
     */
    public static Duration roundToMillis(Duration duration) {
        return duration == null ? null : Duration.ofMillis(duration.plus(Duration.ofNanos(500_000)).toMillis());

    }

}
