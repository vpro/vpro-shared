package nl.vpro.rs.converters;

import java.time.Duration;

import javax.ws.rs.ext.ParamConverter;

import nl.vpro.util.TimeUtils;


/**
 * @author Michiel Meeuwissen
 * @since 0.28
 */

public class DurationParamConverter implements ParamConverter<Duration> {

    public static DurationParamConverter INSTANCE = new DurationParamConverter();

    @Override
    public Duration fromString(String value) {
        return TimeUtils.parseDuration(value).orElse(null);


    }

    @Override
    public String toString(Duration value) {
        if (value == null) {
           return null;
        }
        return value.toString();
    }
}
