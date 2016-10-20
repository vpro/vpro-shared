package nl.vpro.util;

import java.time.*;
import java.time.format.DateTimeParseException;
import java.util.Optional;

import org.apache.commons.lang3.StringUtils;

/**
 * @author Michiel Meeuwissen
 * @since 0.45
 */
public class TimeUtils {

    public static ZoneId ZONE_ID = ZoneId.of("Europe/Amsterdam");


    public static Optional<ZonedDateTime> parseZoned(String parse) {
        if (StringUtils.isBlank(parse)) {
            return Optional.empty();
        }
        try {
            return Optional.of(ZonedDateTime.parse(parse));
        } catch (DateTimeParseException dtf) {

        }
        Instant instant = parse(parse).orElse(null);
        if (instant == null) {
            return Optional.empty();
        }
        return Optional.of(instant.atZone(ZONE_ID));
    }

    public static Optional<Instant> parse(String dateValue) {
        if (StringUtils.isBlank(dateValue)) {
            return Optional.empty();
        }


        try {
            return Optional.of(LocalDate.parse(dateValue).atStartOfDay().atZone(ZONE_ID).toInstant());
        } catch (DateTimeParseException dpe) {

        }
        try {
            return Optional.of(LocalDateTime.parse(dateValue).atZone(ZONE_ID).toInstant());
        } catch (DateTimeParseException dpe) {

        }
        //return Instant.parse(dateValue);
        try {
            return Optional.of(OffsetDateTime.parse(dateValue).toInstant());
        } catch (DateTimeParseException dtp) {

        }
        try {
            return Optional.of(ZonedDateTime.parse(dateValue).toInstant());
        } catch (DateTimeParseException dtp) {
        }
        DateTimeParseException dtp;
        try {
            return Optional.of(Instant.parse(dateValue));
        } catch (DateTimeParseException e) {
            dtp = e;
        }

        try {
            Long longValue = Long.parseLong(dateValue);
            if (longValue > 1000 && longValue < 9999) {
                return Optional.of(LocalDate.of(longValue.intValue(), 1, 1).atStartOfDay().atZone(ZONE_ID).toInstant());
            } else {
                return Optional.of(Instant.ofEpochMilli(Long.parseLong(dateValue)));
            }
        } catch (NumberFormatException nfe) {
            throw dtp;
        }

    }

    public static Optional<Duration> parseDuration(String d) {
        if (StringUtils.isBlank(d)) {
            return Optional.empty();
        }
        try {
            return Optional.of(Duration.parse(d));
        } catch (DateTimeParseException dtp) {
            try {
                return Optional.of(Duration.ofMillis(Long.parseLong(d)));
            } catch (NumberFormatException nfe) {
                // ignore
            }
            if (!d.startsWith("P")) {
                return parseDuration("P" + d);
            } else if (!d.startsWith("PT")){
                return parseDuration("PT" + d.substring(1));
            }
            throw dtp;
        }
    }

}
