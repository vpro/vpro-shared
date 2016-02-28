package nl.vpro.persistence;

import javax.persistence.AttributeConverter;
import javax.persistence.Converter;
import java.sql.Time;
import java.time.Duration;
import java.time.temporal.ChronoUnit;

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
        return timestamp == null ? null : Duration.of(timestamp.getTime(), ChronoUnit.MILLIS);
    }
}
