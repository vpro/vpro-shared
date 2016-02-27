package nl.vpro.persistence;

import javax.persistence.AttributeConverter;
import javax.persistence.Converter;
import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;

/**
 * @author Michiel Meeuwissen
 * @since 0.36
 */
@Converter
public class DurationToTimestampConverter implements AttributeConverter<Duration, Timestamp> {

    @Override
    public Timestamp convertToDatabaseColumn(Duration duration) {
        return duration == null ? null : new Timestamp(duration.get(ChronoUnit.MILLIS));
    }

    @Override
    public Duration convertToEntityAttribute(Timestamp timestamp) {
        return timestamp == null ? null : Duration.of(timestamp.getTime(), ChronoUnit.MILLIS);
    }
}
