package nl.vpro.xml.bind;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;
import java.util.Locale;

import javax.xml.bind.annotation.adapters.XmlAdapter;

import static nl.vpro.xml.util.XmlUtils.DEFAULT_ZONE;

/**
 * https://bugs.openjdk.java.net/browse/JDK-8042456
 * @author Michiel Meeuwissen
 * @since 0.21
 */
public class ZonedDateTimeXmlAdapter extends XmlAdapter<String, ZonedDateTime> {

    public static final ThreadLocal<Boolean> OMIT_MILLIS_IF_ZERO = ThreadLocal.withInitial(() -> true);

    private final DateTimeFormatter formatter =
        DateTimeFormatter
            .ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSZZZZZ")
            .withLocale(Locale.US);

    private final DateTimeFormatter formatterNoMillis =
        DateTimeFormatter
            .ofPattern("yyyy-MM-dd'T'HH:mm:ssZZZZZ")
            .withLocale(Locale.US);


    @Override
    public ZonedDateTime unmarshal(String dateValue) {
        if (dateValue == null) {
            return null;
        }
        try {
            return LocalDate.parse(dateValue).atStartOfDay().atZone(DEFAULT_ZONE);
        } catch (DateTimeParseException dpe) {

        }
        try {
            return LocalDateTime.parse(dateValue).atZone(DEFAULT_ZONE);
        } catch (DateTimeParseException dpe) {

        }
        return ZonedDateTime.parse(dateValue);
    }

    @Override
    public String marshal(ZonedDateTime value) {
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
