/*
 * Copyright (C) 2012 All rights reserved
 * VPRO The Netherlands
 */
package nl.vpro.util;

import java.time.*;
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

    public static Date MIN_VALUE = new Date(Long.MIN_VALUE);

    public static Date MAX_VALUE = new Date(Long.MAX_VALUE);

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
        return first.before(second) ? smallest ? first : second : smallest ? second : first;
    }

    public static Date nullIsMinimal(Date date) {
        return date == null ? MIN_VALUE : date;
    }


    public static Date nullIsMaximal(Date date) {
        return date == null ? MAX_VALUE : date;
    }

    public static Date minimalIsNull(Date date) {
        return MIN_VALUE.equals(date) ? null : date;
    }


    public static Date maximalIsNull(Date date) {
        return MAX_VALUE.equals(date) ? null : date;
    }

    public static Instant toInstant(Date date) {
        if (date instanceof java.sql.Date || date instanceof  java.sql.Time) {
            date = new Date(date.getTime());
        }
        return date == null ? null : date.toInstant();
    }


    public static Date toDate (Instant date) {
        return date == null ? null : Date.from(date);
    }

    public static Date toDate(ZonedDateTime date) {
        return date == null ? null : Date.from(date.toInstant());
    }

    public static Long toLong(Instant instant) {
        return instant == null ? null : instant.toEpochMilli();
    }

    public static Long toLong(Date date) {
        return date == null ? null : date.getTime();
    }


    public static Date toDate(Duration duration) {
        return duration == null ? null : new Date(duration.toMillis());
    }


    public static Date toDate(LocalDateTime date, ZoneId zoneId) {
        return date == null ? null : Date.from(date.atZone(zoneId).toInstant());
    }

    public static boolean isAfter(Instant instant1, Instant instant2) {
        if (instant1 == null || instant2 == null) {
            return false;
        }
        return instant1.isAfter(instant2);
    }

}
