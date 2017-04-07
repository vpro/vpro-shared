package nl.vpro.persistence;

import java.time.Duration;

import javax.persistence.AttributeConverter;
import javax.persistence.Converter;

/**
 * Postgresql has a dedicated Interval type. I tried to get it working with something like this. It didn't work, I couldn't get hibernate to not report:
 * Invocation of init method failed; nested exception is org.hibernate.HibernateException: Wrong column type in public.subtitles for column offset. Found: interval, expected: varchar(255)
 *
 * @author Michiel Meeuwissen
 * @since 0.47
 */
@Converter
public class DurationToIntervalConverter implements AttributeConverter<Duration, String> {

    @Override
    public String convertToDatabaseColumn(Duration duration) {
        return duration == null ? null : duration.toString();
    }

    @Override
    public Duration convertToEntityAttribute(String timestamp) {
        return timestamp == null ? null : Duration.parse(timestamp);
    }
}
