/**
 * Copyright (C) 2012 All rights reserved
 * VPRO The Netherlands
 */
package nl.vpro.util;

import java.util.Date;
import java.util.GregorianCalendar;
import java.util.TimeZone;

import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;

/**
 * @author Roelof Jan Koekoek
 * @since 0.11
 */
public class DateUtils {
    public static XMLGregorianCalendar toXmlGregorianCalendar(Date date) throws DatatypeConfigurationException {
        return toXmlGregorianCalendar(date, TimeZone.getDefault());
    }

    public static XMLGregorianCalendar toXmlGregorianCalendar(Date date, TimeZone timeZone) throws DatatypeConfigurationException {
        GregorianCalendar calendar = new GregorianCalendar(timeZone);
        calendar.setTime(date);
        return DatatypeFactory.newInstance().newXMLGregorianCalendar(calendar);
    }

    public static Date lowest(Date first, Date second) {
        return compare(first, second, true);
    }

    public static Date highest(Date first, Date second) {
        return compare(first, second, false);
    }

    private static Date compare(Date first, Date second, boolean smallest) {
        if(first == null) {
            return second;
        }
        if(second == null) {
            return first;
        }
        return first.getTime() < second.getTime() ? smallest ? first : second : smallest ? second : first;
    }
}
