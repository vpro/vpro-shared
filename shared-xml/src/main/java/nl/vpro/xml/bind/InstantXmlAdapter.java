package nl.vpro.xml.bind;

import java.time.*;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Locale;

import javax.xml.bind.annotation.adapters.XmlAdapter;

/**
 * https://bugs.openjdk.java.net/browse/JDK-8042456
 * Use like so
 * <pre>{@code
 * @XmlAttribute
 * @XmlJavaTypeAdapter(InstantXmlAdapter.class)
 * @XmlSchemaType(name = "dateTime")
 * private Instant lastModified;
 *}</pre>
 * @author Michiel Meeuwissen
 * @since 0.21
 */
public class InstantXmlAdapter extends XmlAdapter<String, Instant> {

    public static ZoneId ZONE = ZoneId.of("Europe/Amsterdam");

    private final DateTimeFormatter formatter =
        DateTimeFormatter
            .ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSZZZZZ")
            .withLocale(Locale.US)
            .withZone(ZONE);



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
        //return Instant.parse(dateValue);
        try {
            return OffsetDateTime.parse(dateValue).toInstant();
        } catch (DateTimeParseException dtp) {

        }
        try {
            return Instant.parse(dateValue);
        } catch (DateTimeParseException dtp) {
            return Instant.ofEpochMilli(Long.parseLong(dateValue));
        }
    }

    @Override
    public String marshal(Instant value) {
        return value != null ? formatter.format(value) : null;
    }
}
