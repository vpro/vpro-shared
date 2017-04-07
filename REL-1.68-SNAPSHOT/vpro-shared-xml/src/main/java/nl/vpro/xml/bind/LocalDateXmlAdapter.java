package nl.vpro.xml.bind;

import java.time.LocalDate;
import java.time.Year;
import java.time.format.DateTimeParseException;
import java.time.temporal.Temporal;

import javax.xml.bind.annotation.adapters.XmlAdapter;

/**
 * https://bugs.openjdk.java.net/browse/JDK-8042456
 * @author Michiel Meeuwissen
 * @since 0.28
 */
public class LocalDateXmlAdapter extends XmlAdapter<String, Temporal> {



    @Override
    public Temporal unmarshal(String dateValue) {
        if (dateValue == null) {
            return null;
        }
        try {
            return LocalDate.parse(dateValue);
        } catch (DateTimeParseException pe) {
            return Year.parse(dateValue);
        }
    }

    @Override
    public String marshal(Temporal value) {
        return value != null ? value.toString() : null;
    }
}
