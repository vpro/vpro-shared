package nl.vpro.xml.bind;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.Locale;

import javax.xml.bind.annotation.adapters.XmlAdapter;

import nl.vpro.util.TimeUtils;

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

    public static final ThreadLocal<Boolean> OMIT_MILLIS_IF_ZERO = ThreadLocal.withInitial(() -> true);

    public static ZoneId ZONE = ZoneId.of("Europe/Amsterdam");

    private final static DateTimeFormatter formatter =
        DateTimeFormatter
            .ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSZZZZZ")
            .withLocale(Locale.US)
            .withZone(ZONE);

    private final static DateTimeFormatter formatterNoMillis =
        DateTimeFormatter
            .ofPattern("yyyy-MM-dd'T'HH:mm:ssZZZZZ")
            .withLocale(Locale.US)
            .withZone(ZONE);



    @Override
    public Instant unmarshal(String dateValue) {
        return TimeUtils.parse(dateValue).orElse(null);
    }

    @Override
    public String marshal(Instant value) {
       return toXMLFormat(value);
    }

    public static String toXMLFormat(Instant value) {
        if (value == null) {
            return null;
        }
        if (value.getNano() == 0 && OMIT_MILLIS_IF_ZERO.get()) {
            return formatterNoMillis.format(value);
        } else {
            return formatter.format(
                // round to millis
                value.plusNanos(500000).truncatedTo(ChronoUnit.MILLIS)
            );
        }
    }
}
