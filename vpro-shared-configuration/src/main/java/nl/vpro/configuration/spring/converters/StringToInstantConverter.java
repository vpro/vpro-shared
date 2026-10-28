package nl.vpro.configuration.spring.converters;

import java.time.Instant;

import org.jspecify.annotations.NonNull;
import org.meeuw.time.TimeUtils;
import org.springframework.core.convert.converter.Converter;


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
