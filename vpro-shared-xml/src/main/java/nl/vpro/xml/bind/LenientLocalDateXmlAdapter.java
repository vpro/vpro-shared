package nl.vpro.xml.bind;

import lombok.extern.slf4j.Slf4j;

import java.time.format.DateTimeParseException;
import java.time.temporal.Temporal;

/**
 * As {@link LocalDateXmlAdapter}, only unparsable values will be unmarshalled to <code>null</code>
 * @author Michiel Meeuwissen
 * @since 2.4
 */
@Slf4j
public class LenientLocalDateXmlAdapter extends LocalDateXmlAdapter {

    @Override
    public Temporal unmarshal(String dateValue) {
        try {
            return super.unmarshal(dateValue);
        } catch (DateTimeParseException dte) {
            log.warn(dte.getMessage());
            return null;
        }
    }
}
