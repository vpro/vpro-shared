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
            return Optional.of(Instant.parse(dateValue));
        } catch (DateTimeParseException dtp) {
            return Optional.of(Instant.ofEpochMilli(Long.parseLong(dateValue)));
        }

    }

}
