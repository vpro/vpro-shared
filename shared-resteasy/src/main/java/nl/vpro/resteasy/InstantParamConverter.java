package nl.vpro.resteasy;

import java.time.Instant;
import java.util.regex.Pattern;

import javax.ws.rs.ext.ParamConverter;
import javax.ws.rs.ext.Provider;


/**
 * @author Michiel Meeuwissen
 * @since 0.31
 */
@Provider
public class InstantParamConverter implements ParamConverter<Instant> {

    static InstantParamConverter INSTANCE = new InstantParamConverter();
    private static final Pattern NUMERIC = Pattern.compile("\\d+");

    @Override
    public Instant fromString(String value) {
        if (value == null || value.length() == 0) {
            return null;
        }
        if (NUMERIC.matcher(value).matches()) {
            return Instant.ofEpochMilli(Long.valueOf(value));
        }
        return Instant.parse(value);


    }

    @Override
    public String toString(Instant value) {
        if (value == null) {
           return null;
        }
        return value.toString();
    }
}
