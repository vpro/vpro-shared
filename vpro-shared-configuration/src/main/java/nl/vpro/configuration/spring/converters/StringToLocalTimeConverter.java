package nl.vpro.configuration.spring.converters;

import java.time.LocalTime;

import org.checkerframework.checker.nullness.qual.NonNull;
import org.springframework.core.convert.converter.Converter;

import nl.vpro.util.TimeUtils;

/**
 * Converts a {@link String} into an {@link LocalTime} using {@link TimeUtils#parseLocalTime(CharSequence)}
 * @author Michiel Meeuwissen
 * @since 2.13
 */
public class StringToLocalTimeConverter implements Converter<String, LocalTime> {

    @Override
    public LocalTime convert(@NonNull String s) {
        return TimeUtils.parseLocalTime(s);
    }
}
