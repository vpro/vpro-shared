package nl.vpro.configuration.spring.converters;

import java.time.LocalDateTime;

import org.jspecify.annotations.NonNull;
import org.meeuw.time.TimeUtils;
import org.springframework.core.convert.converter.Converter;


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
