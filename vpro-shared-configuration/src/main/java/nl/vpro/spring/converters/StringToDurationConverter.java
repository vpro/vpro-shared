package nl.vpro.spring.converters;

import java.time.Duration;

import org.checkerframework.checker.nullness.qual.NonNull;

import org.springframework.core.convert.converter.Converter;

import nl.vpro.util.TimeUtils;

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
