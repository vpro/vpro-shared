package nl.vpro.configuration.spring.converters;

import org.checkerframework.checker.nullness.qual.NonNull;
import org.springframework.core.convert.converter.Converter;

/**
 * Convert String to Integer, allowing underscores and surrounding spaces.
 * @author Michiel Meeuwissen
 * @since 5.14.1
 */
public class StringToIntegerConverter implements Converter<String, Integer> {

    @Override
    public Integer convert(@NonNull String text) {
        String cleaned = text.trim().replace("_", "");
        return Integer.valueOf(cleaned);
    }
}
