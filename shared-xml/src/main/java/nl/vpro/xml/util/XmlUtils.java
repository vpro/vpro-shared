package nl.vpro.xml.util;

import java.time.Instant;
import java.util.Date;

import javax.xml.datatype.XMLGregorianCalendar;

/**
 * @author Michiel Meeuwissen
 * @since 0.36
 */
public class XmlUtils {

    public static Date toDate(XMLGregorianCalendar in) {
        return in == null ? null : in.toGregorianCalendar().getTime();
    }

    public static Instant toInstant(XMLGregorianCalendar in) {
        return in == null ? null : in.toGregorianCalendar().getTime().toInstant();
    }
}
