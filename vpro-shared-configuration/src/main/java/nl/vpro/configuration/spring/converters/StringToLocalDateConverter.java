package nl.vpro.configuration.spring.converters;

import java.time.LocalDate;

import org.checkerframework.checker.nullness.qual.NonNull;
import org.springframework.core.convert.converter.Converter;

/**
 * Converts a {@link String} into an {@link LocalDate} using {@link LocalDate#parse(CharSequence)} (CharSequence)}
 * @author Michiel Meeuwissen
 * @since 5.4
 */
public class StringToLocalDateConverter implements Converter<String, LocalDate> {

    @Override
    public LocalDate convert(@NonNull String s) {
        return LocalDate.parse(s);
    }
}
