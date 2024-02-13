package nl.vpro.configuration.spring.converters;

import java.time.Instant;

import org.checkerframework.checker.nullness.qual.NonNull;
import org.springframework.core.convert.converter.Converter;

import nl.vpro.util.TimeUtils;

/**
 * Converts a {@link String} into an {@link Instant} using {@link TimeUtils#parse(CharSequence)}
 * @author Michiel Meeuwissen
 * @since 2.19
 */
public class StringToInstantConverter implements Converter<String, Instant> {

    @Override
    public Instant convert(@NonNull String s) {
        return TimeUtils.parse(s).orElseThrow(() -> new IllegalArgumentException("Cannot parse " + s));
    }
}
