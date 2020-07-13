package nl.vpro.util;

import lombok.extern.slf4j.Slf4j;

import java.time.*;
import java.time.format.DateTimeParseException;
import java.time.temporal.TemporalAmount;
import java.util.Date;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;

/**
 * @author Michiel Meeuwissen
 * @since 0.45
 */
@Slf4j
public class TimeUtils {

    public static final ZoneId ZONE_ID =  ZoneId.of("Europe/Amsterdam");


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
            // this is the proper XML representation to try that first
            return Optional.of(OffsetDateTime.parse(dateValue).toInstant());
        } catch (DateTimeParseException ignored) {

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
            long longValue = Long.parseLong(dateValue.toString());
            if (longValue >= 1000 && longValue <= 9999) {
                return Optional.of(LocalDate.of((int) longValue, 1, 1).atStartOfDay().atZone(ZONE_ID).toInstant());
            } else {
                return Optional.of(Instant.ofEpochMilli(Long.parseLong(dateValue.toString())));
            }
        } catch (NumberFormatException nfe) {
            throw dtp;
        }

    }

    /**
     * Parses a {@link CharSequence} to a duration. This begins for checking emptyness (and returns then an empty {@link Optional} .
     * Then it basicly calls {@link Duration#parse(CharSequence)}, but if that fails it has several fall backs:
     * <ul>
     *     <li>Also a week notation like <code>PT2W</code> as defined by ISO-8601 is supported</li>
     *     <li>Possible white space breaking the parsing will be ignored</li>
     *     <li>If the string looks like an unresolved (spring EL) variable, we'll parse to the empty optional</li>
     *     <li>If the string parses as a {@link Long}, it will be interpreted as a number of milliseconds</li>
     *     <li>If the string would become a valid ISO-8601 duration by prefixing it with 'P' or 'PT', that will be done too, making those prefixes effectively optional, so that the string like '1s' will parse to one second.</li>
     * </ul>
     * @see #parseDuration(CharSequence)
     */
    public static Optional<Duration> parseDuration(CharSequence d) {
        return parseDuration(null, d);
    }

    private static final Pattern WEEKS = Pattern.compile("^P(\\d+)W$");

    private static Optional<Duration> parseDuration(DateTimeParseException original, CharSequence d) {
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
            if (original != null) {
                dtp = original;
            }
            // For some reason Duration.parse does not support ISO_8601's PnW format (https://en.wikipedia.org/wiki/ISO_8601#Durations)
            Matcher matcher = WEEKS.matcher(d);
            if (matcher.matches()) {
                return Optional.of(Duration.ofDays(7L * Integer.parseInt(matcher.group(1))));
            }

            String ds = d.toString().replaceAll("\\s*", "");
            if (ds.length() < d.length()) {
                return parseDuration(dtp, ds);
            }

            try {
                return Optional.of(Duration.ofMillis(Long.parseLong(ds)));
            } catch (NumberFormatException nfe) {
                // ignore
            }
            if (!ds.startsWith("P")) {
                return parseDuration(dtp, "P" + ds);
            } else if (!ds.startsWith("PT")){
                // so it did start with P, just not with PT, and it couldn't be parsed
                return parseDuration(dtp, "PT" + ds.substring(1));
            }

            throw new DateTimeParseException(dtp.getParsedString() + ":" + dtp.getMessage(), dtp.getParsedString(), dtp.getErrorIndex());
        }
    }


    /**
     * Parses a {@link CharSequence} to a {@link TemporalAmount}. First using {@link Period#parse(CharSequence)}, and than, if that fails, using
     * {@link #parseDuration(CharSequence)}
     */
    public static Optional<? extends TemporalAmount> parseTemporalAmount(CharSequence d) {
        try {
            return Optional.of(Period.parse(d));
        } catch (DateTimeParseException dte) {
            return parseDuration(dte, d);
        }

    }

    public static String toParsableString(Duration duration) {
        return duration.toString().substring(2);
    }
    /**
     * @since 2.6
     */
    public static Optional<LocalDateTime> parseLocalDateTime(CharSequence d) {
        if (StringUtils.isBlank(d)) {
            return Optional.empty();
        }
        if (d.toString().startsWith("${")) {// unresolved spring setting;
            log.warn("Found {} as localdatetime, returing empty", d);
            return Optional.empty();
        }


        try {
            return Optional.of(LocalDateTime.parse(d));
        } catch (DateTimeParseException dtp) {
            try {
                return Optional.of(LocalDate.parse(d).atStartOfDay());
            } catch (DateTimeParseException dtp2) {
                throw new DateTimeParseException(dtp.getParsedString() + ":" + dtp.getMessage(), dtp.getParsedString(), dtp.getErrorIndex());
            }
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

    public static Optional<Integer> toSecondsInteger(Duration d) {
        return Optional.ofNullable(d == null ? null : (int) (d.toMillis() / 1000));
    }

    public static Optional<Float> toSeconds(Duration d) {
        return Optional.ofNullable(d == null ? null : d.toMillis() / 1000f);
    }

    public static Optional<Long> toMillis(Duration d) {
        return Optional.ofNullable(d == null ? null : d.toMillis());
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
