/**
 * Copyright (C) 2008 All rights reserved
 * VPRO The Netherlands
 */
package nl.vpro.domain.shared.bind;

import java.util.Date;

import javax.xml.bind.annotation.adapters.XmlAdapter;
import javax.xml.datatype.DatatypeConfigurationException;
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
        return datatypeFactory.newDurationDayTime(time);
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
}
