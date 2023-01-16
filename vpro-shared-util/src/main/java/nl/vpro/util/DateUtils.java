/*
 * Copyright (C) 2012 All rights reserved
 * VPRO The Netherlands
 */
package nl.vpro.util;

import java.time.Duration;
import java.time.*;
import java.util.*;

import javax.xml.datatype.*;

import org.checkerframework.checker.nullness.qual.*;

import com.google.common.collect.Range;

/**
 * @author Roelof Jan Koekoek
 * @since 0.11
 */
public class DateUtils {

    private static final Date MIN_VALUE = new Date(Long.MIN_VALUE);

    private static final Date MAX_VALUE = new Date(Long.MAX_VALUE);

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

    @NonNull
    public static Date nullIsMinimal(@Nullable Date date) {
        return date == null ? MIN_VALUE : date;
    }

    @NonNull
    public static Date nullIsMaximal(@Nullable Date date) {
        return date == null ? MAX_VALUE : date;
    }

    @Nullable
    public static Date minimalIsNull(Date date) {
        return MIN_VALUE.equals(date) ? null : date;
    }

    @Nullable
    public static Date maximalIsNull(@Nullable Date date) {
        return MAX_VALUE.equals(date) ? null : date;
    }

    @PolyNull
    public static Instant toInstant(@PolyNull Date date) {
        if (date instanceof java.sql.Date || date instanceof  java.sql.Time) {
            date = new Date(date.getTime());
        }
        return date == null ? null : date.toInstant();
    }

    @PolyNull
    public static Date toDate (@PolyNull Instant date) {
        return date == null ? null : Date.from(date);
    }
    @PolyNull
    public static Date toDate(@PolyNull ZonedDateTime date) {
        return date == null ? null : Date.from(date.toInstant());
    }
    @PolyNull
    public static Instant toInstant(@PolyNull ZonedDateTime date) {
        return date == null ? null : date.toInstant();
    }

    @PolyNull
    public static Long toLong(@PolyNull Instant instant) {
        return instant == null ? null : instant.toEpochMilli();
    }

    @PolyNull
    public static Long toLong(@PolyNull Date date) {
        return date == null ? null : date.getTime();
    }

    @PolyNull
    public static Date toDate(@PolyNull Duration duration) {
        return duration == null ? null : new Date(duration.toMillis());
    }

    @PolyNull
    public static Date toDate(@PolyNull LocalDateTime date, ZoneId zoneId) {
        return date == null ? null : Date.from(date.atZone(zoneId).toInstant());
    }

    @PolyNull
    public static Instant toInstant(@PolyNull LocalDateTime date, ZoneId zoneId) {
        return date == null ? null : date.atZone(zoneId).toInstant();
    }


    @PolyNull
    public static LocalDateTime toLocalDateTime(@PolyNull Instant date, ZoneId zoneId) {
        return date == null ? null : date.atZone(zoneId).toLocalDateTime();
    }

    /**
     * {@code null}-safe version of {@link Instant#isAfter(Instant)}
     */
    public static boolean isAfter(@Nullable Instant instant1, @Nullable Instant instant2) {
        if (instant1 == null || instant2 == null) {
            return false;
        }
        return instant1.isAfter(instant2);
    }

    public static Range<LocalDateTime> toLocalDateTimeRange(@NonNull Range<Instant> instantRange, @NonNull ZoneId zonedId) {
        return Ranges.convert(instantRange, i -> i.atZone(zonedId).toLocalDateTime());
    }

}
