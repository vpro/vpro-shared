package nl.vpro.xml.util;

import java.time.Instant;
import java.util.Date;
import java.util.GregorianCalendar;

import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;

/**
 * @author Michiel Meeuwissen
 * @since 0.36
 */
public class XmlUtils {

    static final DatatypeFactory FACTORY;
    static {
        try {
            FACTORY = DatatypeFactory.newInstance();
        } catch (DatatypeConfigurationException e) {
            throw new IllegalStateException();
        }
    }

    public static Date toDate(XMLGregorianCalendar in) {
        return in == null ? null : in.toGregorianCalendar().getTime();
    }

    public static Instant toInstant(XMLGregorianCalendar in) {
        return in == null ? null : in.toGregorianCalendar().getTime().toInstant();
    }

    public static XMLGregorianCalendar toXml(Date date) {
        if (date == null) {
            return null;
        }
        GregorianCalendar c = new GregorianCalendar();
        c.setTime(date);
        return FACTORY.newXMLGregorianCalendar(c);
    }

    public static XMLGregorianCalendar toXml(Instant date) {
        if (date == null) {
            return null;
        }
        return toXml(Date.from(date));
    }
}
