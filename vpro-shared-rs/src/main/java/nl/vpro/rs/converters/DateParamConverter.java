package nl.vpro.rs.converters;

import java.util.Calendar;
import java.util.Date;

import javax.ws.rs.ext.ParamConverter;
import javax.xml.bind.DatatypeConverter;


/**
 * @author Michiel Meeuwissen
 * @since 0.23
 */

public class DateParamConverter implements ParamConverter<Date> {

    static DateParamConverter INSTANCE = new DateParamConverter();

    @Override
    public Date fromString(String value) {
        if (value == null || value.length() == 0) {
            return null;
        }
        return DatatypeConverter.parseDateTime(value).getTime();


    }

    @Override
    public String toString(Date value) {
        if (value == null) {
           return null;
        }
        Calendar cal = Calendar.getInstance();
        cal.setTime(value);
        return DatatypeConverter.printDateTime(cal);

    }
}
