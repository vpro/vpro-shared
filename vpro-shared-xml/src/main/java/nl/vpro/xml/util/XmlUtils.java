package nl.vpro.xml.util;

import lombok.extern.slf4j.Slf4j;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Locale;
import java.util.TimeZone;

import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeConstants;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;

/**
 * @author Michiel Meeuwissen
 * @since 0.36
 */
@Slf4j
public class XmlUtils {

    public static ZoneId DEFAULT_ZONE = ZoneId.of("Europe/Amsterdam");
    static final DatatypeFactory FACTORY;
    static {
        try {
            FACTORY = DatatypeFactory.newInstance();
        } catch (DatatypeConfigurationException e) {
            throw new IllegalStateException();
        }
    }

    @Deprecated
    public static Date toDate(XMLGregorianCalendar in) {
        return in == null ? null : in.toGregorianCalendar().getTime();
    }

    public static Instant toInstant(ZoneId defaultZoneId, XMLGregorianCalendar in) {
        if (in == null) {
            return null;
        }
        if (in.getTimezone() == DatatypeConstants.FIELD_UNDEFINED) {
            TimeZone zone;
            if (defaultZoneId != null) {
                zone = TimeZone.getTimeZone(defaultZoneId);
            } else {
                zone = TimeZone.getTimeZone(DEFAULT_ZONE);
                log.info("{} defines no timezone. Falling back to {}", in, DEFAULT_ZONE);
            }
            return in.toGregorianCalendar(zone, Locale.US, in).getTime().toInstant();
        } else {
            return in.toGregorianCalendar().getTime().toInstant();
        }
    }

    public static ZonedDateTime toZonedDateTime(ZoneId zoneId, XMLGregorianCalendar in) {
        return toInstant(zoneId, in).atZone(zoneId);
    }

    public static OffsetDateTime toOffsetDateTime(ZoneId zoneId, XMLGregorianCalendar in) {
        return toInstant(zoneId, in).atZone(zoneId).toOffsetDateTime();
    }

    /**
     * @deprecated No explicit default time zone, makes parsing unreliable.
     */
    @Deprecated
    public static Instant toInstant(XMLGregorianCalendar in) {
        return toInstant(null, in);
    }

    @Deprecated
    public static XMLGregorianCalendar toXml(Date date) {
        if (date == null) {
            return null;
        }
        return toXml(DEFAULT_ZONE, date.toInstant());
    }

    public static XMLGregorianCalendar toXml(ZoneId zoneId, Instant date) {
        if (date == null) {
            return null;
        }
        GregorianCalendar c = new GregorianCalendar();
        c.setTime(Date.from(date));
        if (zoneId != null) {
            c.setTimeZone(TimeZone.getTimeZone(zoneId));
        }
        return FACTORY.newXMLGregorianCalendar(c);
    }

}
