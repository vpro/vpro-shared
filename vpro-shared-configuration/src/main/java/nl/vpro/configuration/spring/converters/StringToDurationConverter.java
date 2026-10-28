package nl.vpro.configuration.spring.converters;

import java.time.Duration;

import org.jspecify.annotations.NonNull;
import org.meeuw.time.TimeUtils;
import org.springframework.core.convert.converter.Converter;


/**
 * Converts a {@link String} into an {@link Duration} using {@link TimeUtils#parseDuration(CharSequence)}
 * @author Michiel Meeuwissen
 * @since 2.7
 */
public class StringToDurationConverter implements Converter<String, Duration> {

    @Override
    public Duration convert(@NonNull String s) {
        return TimeUtils.parseDurationOrThrow(s);
    }
}
