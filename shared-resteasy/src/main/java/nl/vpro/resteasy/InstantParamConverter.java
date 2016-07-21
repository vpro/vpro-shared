package nl.vpro.resteasy;

import java.time.Instant;
import java.time.ZoneId;
import java.util.regex.Pattern;

import javax.ws.rs.ext.ParamConverter;
import javax.ws.rs.ext.Provider;

import nl.vpro.util.TimeUtils;


/**
 * @author Michiel Meeuwissen
 * @since 0.31
 */
@Provider
public class InstantParamConverter implements ParamConverter<Instant> {

    private static ZoneId ZONE_ID = ZoneId.of("Europe/Amsterdam");

    static InstantParamConverter INSTANCE = new InstantParamConverter();
    private static final Pattern NUMERIC = Pattern.compile("\\d+");

    @Override
    public Instant fromString(String value) {
        if (value == null || value.length() == 0) {
            return null;
        }

        return TimeUtils.parse(value).orElse(null);


    }

    @Override
    public String toString(Instant value) {
        if (value == null) {
           return null;
        }
        return value.toString();
    }
}
