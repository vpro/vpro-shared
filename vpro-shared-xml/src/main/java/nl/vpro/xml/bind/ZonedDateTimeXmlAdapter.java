package nl.vpro.xml.bind;

import java.time.*;
import java.time.format.DateTimeParseException;

import javax.xml.bind.annotation.adapters.XmlAdapter;
import static nl.vpro.xml.bind.InstantXmlAdapter.ZONE;

/**
 * https://bugs.openjdk.java.net/browse/JDK-8042456
 * @author Michiel Meeuwissen
 * @since 0.21
 */
public class ZonedDateTimeXmlAdapter extends XmlAdapter<String, ZonedDateTime> {



    @Override
    public ZonedDateTime unmarshal(String dateValue) {
        if (dateValue == null) {
            return null;
        }
        try {
            return LocalDate.parse(dateValue).atStartOfDay().atZone(ZONE);
        } catch (DateTimeParseException dpe) {

        }
        try {
            return LocalDateTime.parse(dateValue).atZone(ZONE);
        } catch (DateTimeParseException dpe) {

        }
        return ZonedDateTime.parse(dateValue);
    }

    @Override
    public String marshal(ZonedDateTime value) {
        return value != null ? value.toString() : null;
    }
}
