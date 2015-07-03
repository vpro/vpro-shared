package nl.vpro.xml.bind;

import java.time.*;
import java.time.format.DateTimeParseException;

import javax.xml.bind.annotation.adapters.XmlAdapter;

/**
 * https://bugs.openjdk.java.net/browse/JDK-8042456
 * @author Michiel Meeuwissen
 * @since 0.21
 */
public class InstantXmlAdapter extends XmlAdapter<String, Instant> {

    public static ZoneId ZONE = ZoneId.of("Europe/Amsterdam");


    @Override
    public Instant unmarshal(String dateValue) {
        if (dateValue == null) {
            return null;
        }
        try {
            return LocalDate.parse(dateValue).atStartOfDay().atZone(ZONE).toInstant();
        } catch (DateTimeParseException dpe) {

        }
        try {
            return LocalDateTime.parse(dateValue).atZone(ZONE).toInstant();
        } catch (DateTimeParseException dpe) {

        }
        return OffsetDateTime.parse(dateValue).toInstant();
    }

    @Override
    public String marshal(Instant value) {
        return value != null ? OffsetDateTime.ofInstant(value, ZONE).toString() : null;
    }
}
