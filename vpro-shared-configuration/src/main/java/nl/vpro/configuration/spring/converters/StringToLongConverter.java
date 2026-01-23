package nl.vpro.configuration.spring.converters;

import org.checkerframework.checker.nullness.qual.NonNull;
import org.springframework.core.convert.converter.Converter;

/**
 * Convert String to Long, allowing underscores and surrounding spaces.
 * @author Michiel Meeuwissen
 * @since 5.14.1
 */
public class StringToLongConverter implements Converter<String, Long> {

    @Override
    public Long convert(@NonNull String text) {
        String cleaned = text.trim().replace("_", "");
        return Long.valueOf(cleaned);
    }
}
