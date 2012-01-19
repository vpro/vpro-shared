/**
 * Copyright (C) 2008 All rights reserved
 * VPRO The Netherlands
 */
package nl.vpro.domain.shared.bind;

import java.util.Date;

import javax.xml.bind.annotation.adapters.XmlAdapter;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.Duration;

public class DateToDuration extends XmlAdapter<Duration, Date> {


    @Override
    public Duration marshal(Date date) throws Exception {
        DatatypeFactory datatypeFactory = DatatypeFactory.newInstance();
        long time = date.getTime();
        Duration dur;
        if (time < 30l * 24 * 60 * 60 * 1000) {
            System.out.println("Using DayTime duration for " + date);
            dur = datatypeFactory.newDurationDayTime(time);
        } else {
            System.out.println("Using normal duration for " + date);
            dur = datatypeFactory.newDuration(time);
        }

        return dur;
    }

    @Override
    public Date unmarshal(Duration duration) throws Exception {
        Date date = new Date(0);
        duration.addTo(date);
        return date;
    }
}
