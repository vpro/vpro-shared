package nl.vpro.resteasy;

import java.time.Duration;

import javax.ws.rs.ext.ParamConverter;


/**
 * @author Michiel Meeuwissen
 * @since 0.28
 */

public class DurationParamConverter implements ParamConverter<Duration> {

    static DurationParamConverter INSTANCE = new DurationParamConverter();

    @Override
    public Duration fromString(String value) {
        if (value == null || value.length() == 0) {
            return null;
        }
        return Duration.parse(value);


    }

    @Override
    public String toString(Duration value) {
        if (value == null) {
           return null;
        }
        return value.toString();
    }
}
