package nl.vpro.xml.bind;

import lombok.extern.java.Log;

import java.time.format.DateTimeParseException;
import java.time.temporal.Temporal;

/**
 * As {@link LocalDateXmlAdapter}, only unparsable values will be unmarshalled to <code>null</code>
 * @author Michiel Meeuwissen
 * @since 2.4
 */
@Log
public class LenientLocalDateXmlAdapter extends LocalDateXmlAdapter {

    @Override
    public Temporal unmarshal(String dateValue) {
        try {
            return super.unmarshal(dateValue);
        } catch (DateTimeParseException dte) {
            log.warning(dte.getMessage());
            return null;
        }
    }
}
