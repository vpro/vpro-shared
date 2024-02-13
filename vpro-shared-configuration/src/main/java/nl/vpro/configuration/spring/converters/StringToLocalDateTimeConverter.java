package nl.vpro.configuration.spring.converters;

import java.time.LocalDateTime;

import org.checkerframework.checker.nullness.qual.NonNull;
import org.springframework.core.convert.converter.Converter;

import nl.vpro.util.TimeUtils;

/**
 * Converts a {@link String} into an {@link java.time.LocalDateTime} using {@link TimeUtils#parseLocalDateTime(CharSequence)} (CharSequence)}
 * @author Michiel Meeuwissen
 * @since 2.19
 */
public class StringToLocalDateTimeConverter implements Converter<String, LocalDateTime> {

    @Override
    public LocalDateTime convert(@NonNull String s) {
        return TimeUtils.parseLocalDateTime(s).orElseThrow(() -> new IllegalArgumentException("Cannot parse " + s));
    }
}
