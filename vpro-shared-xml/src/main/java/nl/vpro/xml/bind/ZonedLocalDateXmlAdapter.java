package nl.vpro.xml.bind;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

import javax.xml.bind.annotation.adapters.XmlAdapter;

import static nl.vpro.xml.util.XmlUtils.DEFAULT_ZONE;


/**
 * Formatter a LocalDate as a 'zoned date', using {@link nl.vpro.xml.util.XmlUtils#DEFAULT_ZONE}
 *
 * This makes xml bindings independent of system locale.
 *
 * @author Michiel Meeuwissen
 * @since 2.5
 */
public class ZonedLocalDateXmlAdapter extends XmlAdapter<String, LocalDate> {

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter
            .ofPattern("yyyy-MM-ddZZZZZ")
        .withLocale(Locale.US);


    @Override
    public LocalDate unmarshal(String dateValue) {
        if (dateValue == null) {
            return null;
        }
        return LocalDate.parse(dateValue.substring(0, 10));

    }

    @Override
    public String marshal(LocalDate v) throws Exception {
        return v.atStartOfDay(DEFAULT_ZONE).format(FORMATTER);
    }

}
