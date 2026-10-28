package nl.vpro.configuration.spring.converters;

import java.time.temporal.TemporalAmount;

import org.jspecify.annotations.NonNull;
import org.meeuw.time.TimeUtils;
import org.springframework.core.convert.converter.Converter;


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
