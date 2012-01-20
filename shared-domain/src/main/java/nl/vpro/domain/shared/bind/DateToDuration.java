/**
 * Copyright (C) 2008 All rights reserved
 * VPRO The Netherlands
 */
package nl.vpro.domain.shared.bind;

import java.math.BigDecimal;
import java.util.Calendar;
import java.util.Date;

import javax.xml.bind.annotation.adapters.XmlAdapter;
import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeConstants;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.Duration;

public class DateToDuration extends XmlAdapter<Duration, Date> {


    @Override
    public Duration marshal(Date date) throws Exception {
        long time = date.getTime();
        if (time < 30l * 24 * 60 * 60 * 1000) {
            return marshalDayTime(time);
        } else {
            return marshal(time);
        }
    }

    protected Duration marshalDayTime(long time) throws DatatypeConfigurationException {
        DatatypeFactory datatypeFactory = DatatypeFactory.newInstance();
        return new TimeDuration(datatypeFactory.newDurationDayTime(time));
    }

    protected Duration marshal(long time) throws DatatypeConfigurationException {
        DatatypeFactory datatypeFactory = DatatypeFactory.newInstance();
        return datatypeFactory.newDuration(time);
    }

    @Override
    public Date unmarshal(Duration duration) throws Exception {
        Date date = new Date(0);
        duration.addTo(date);
        return date;
    }


    /**
     * This should be superflous, but test-cases are failing on jenkins.
     * implemented just to correct toString
     */
    private class TimeDuration extends Duration {
        private final Duration dur;
        TimeDuration(Duration dur) {
            this.dur = dur;
        }
        @Override
        public int getSign() {
            return dur.getSign();
        }

        @Override
        public Number getField(DatatypeConstants.Field field) {
            return dur.getField(field);
        }

        @Override
        public boolean isSet(DatatypeConstants.Field field) {
            return dur.isSet(field);
        }

        @Override
        public Duration add(Duration duration) {
            dur.add(duration);
            return this;
        }

        @Override
        public void addTo(Calendar calendar) {
            dur.addTo(calendar);
        }

        @Override
        public Duration multiply(BigDecimal bigDecimal) {
            return dur.multiply(bigDecimal);
        }

        @Override
        public Duration negate() {
            return dur.negate();
        }

        @Override
        public Duration normalizeWith(Calendar calendar) {
            dur.normalizeWith(calendar);
            return this;
        }

        @Override
        public int compare(Duration duration) {
            return dur.compare(duration);
        }

        @Override
        public int hashCode() {
            return dur.hashCode();
        }
        @Override
        public String toString() {
            return String.format("P%dDT%dH%dM%d.%03dS",
                dur.getDays(), dur.getHours(), dur.getMinutes(), dur.getSeconds(),
                dur.getTimeInMillis(new Date(0)) % 1000L);
        }
    }
}
