package nl.vpro.persistence;

import java.time.Duration;

import javax.persistence.AttributeConverter;
import javax.persistence.Converter;

/**
 * @author Michiel Meeuwissen
 * @since 0.47
 */
@Converter
public class DurationToLongConverter implements AttributeConverter<Duration, Long> {

    @Override
    public Long convertToDatabaseColumn(Duration duration) {
        return duration == null ? null : duration.toMillis();
    }

    @Override
    public Duration convertToEntityAttribute(Long timestamp) {
        return timestamp == null ? null : Duration.ofMillis(timestamp);
    }
}
