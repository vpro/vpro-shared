package nl.vpro.rs.converters;

import java.util.Locale;

import jakarta.ws.rs.ext.ParamConverter;


/**
 * Resteasy locale converter sucks a bit ?
 * @author Michiel Meeuwissen
 * @since 0.48
 */

public class LocaleParamConverter implements ParamConverter<Locale> {

    static LocaleParamConverter INSTANCE = new LocaleParamConverter();

    @Override
    public Locale fromString(String value) {
        if (value == null || value.isEmpty()) {
            return null;
        }
        String[] split = value.split("[_-]", 3);
        if (split.length == 1) {
            return new Locale(split[0]);
        } else if (split.length == 2) {
            return new Locale(split[0], split[1]);
        } else {
            return new Locale(split[0], split[1], split[2]);
        }
    }

    @Override
    public String toString(Locale value) {
        if (value == null) {
           return null;
        }
        return value.toString();
    }
}
