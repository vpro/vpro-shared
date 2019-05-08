package nl.vpro.spring.converters;

import java.time.Duration;

import org.springframework.core.convert.converter.Converter;

import nl.vpro.util.TimeUtils;

/**
 * @author Michiel Meeuwissen
 * @since 2.7
 */
public class StringToDurationConverter implements Converter<String, Duration> {
    @Override
    public Duration convert(String s) {
        return TimeUtils.parseDuration(s).orElseThrow(() -> new IllegalArgumentException("Cannot convert to duration " + s));

    }
}
