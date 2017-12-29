package nl.vpro.xml.bind;

import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.Locale;

import javax.xml.bind.annotation.adapters.XmlAdapter;

import nl.vpro.util.TimeUtils;
import nl.vpro.xml.util.XmlUtils;

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

    private final static DateTimeFormatter formatter =
        DateTimeFormatter
            .ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSZZZZZ")
            .withLocale(Locale.US)
            .withZone(XmlUtils.DEFAULT_ZONE);

    private final static DateTimeFormatter formatterNoMillis =
        DateTimeFormatter
            .ofPattern("yyyy-MM-dd'T'HH:mm:ssZZZZZ")
            .withLocale(Locale.US)
            .withZone(XmlUtils.DEFAULT_ZONE);



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
            return formatWithMillis(value);
        }
    }

    static String formatWithMillis(Instant instant) {
        if (instant == null) {
            return null;
        }
        return formatter.format(
            // round to millis
            instant.plusNanos(500_000).truncatedTo(ChronoUnit.MILLIS));
    }
}
