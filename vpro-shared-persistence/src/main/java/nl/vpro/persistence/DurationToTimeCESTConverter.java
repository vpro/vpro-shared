package nl.vpro.persistence;


import lombok.extern.slf4j.Slf4j;

import java.sql.Time;
import java.time.Duration;
import java.util.TimeZone;

import javax.persistence.AttributeConverter;
import javax.persistence.Converter;

/**
 * @author Michiel Meeuwissen
 * @since 1.68.2
 */
@Converter
@Slf4j
public class DurationToTimeCESTConverter implements AttributeConverter<Duration, Time> {

    private static final long OFFSET = TimeZone.getTimeZone("CEST").getOffset(0);
    @Override
    public Time convertToDatabaseColumn(Duration duration) {
        return duration == null ? null : new Time(duration.toMillis());
    }

    @SuppressWarnings("deprecation")
    @Override
    public Duration convertToEntityAttribute(Time timestamp) {
        if (timestamp == null) {
            return null;
        }
        long durationInMillis= timestamp.getTime() - OFFSET;
        return Duration.ofMillis(durationInMillis);
    }
}
