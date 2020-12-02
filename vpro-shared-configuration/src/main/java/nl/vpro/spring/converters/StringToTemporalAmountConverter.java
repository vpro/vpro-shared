package nl.vpro.spring.converters;

import java.time.temporal.TemporalAmount;

import org.checkerframework.checker.nullness.qual.NonNull;
import org.springframework.core.convert.converter.Converter;

import nl.vpro.util.TimeUtils;

/**
 * Converts a {@link String} to a {@link TemporalAmount} using {@link TimeUtils#parseTemporalAmount(CharSequence)}
 * @author Michiel Meeuwissen
 * @since 2.13
 */
public class StringToTemporalAmountConverter implements Converter<String, TemporalAmount> {

    @Override
    public TemporalAmount convert(@NonNull String s) {
        return TimeUtils.parseTemporalAmount(s).orElseThrow(() -> new IllegalArgumentException("Cannot convert to temporal amount " + s));
    }
}
