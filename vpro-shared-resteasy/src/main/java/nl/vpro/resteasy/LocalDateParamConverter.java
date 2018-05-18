package nl.vpro.resteasy;

import java.time.LocalDate;

import javax.ws.rs.ext.ParamConverter;


/**
 * @author Michiel Meeuwissen
 * @since 0.25
 */
public class LocalDateParamConverter implements ParamConverter<LocalDate> {

    static LocalDateParamConverter INSTANCE = new LocalDateParamConverter();

    @Override
    public LocalDate fromString(String value) {
        if (value == null || value.length() == 0) {
            return null;
        }
        return LocalDate.parse(value);


    }

    @Override
    public String toString(LocalDate value) {
        if (value == null) {
           return null;
        }
        return value.toString();
    }
}
