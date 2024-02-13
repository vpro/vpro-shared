package nl.vpro.persistence;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import java.sql.Time;
import java.time.Duration;

/**
 * @author Michiel Meeuwissen
 * @since 0.36
 */
@Converter
public class DurationToTimeConverter implements AttributeConverter<Duration, Time> {

    @Override
    public Time convertToDatabaseColumn(Duration duration) {
        return duration == null ? null : new Time(duration.toMillis());
    }

    @Override
    public Duration convertToEntityAttribute(Time timestamp) {
        return timestamp == null ? null : Duration.ofMillis(timestamp.getTime());
    }
}
