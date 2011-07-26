/**
 * Copyright (C) 2008 All rights reserved
 * VPRO The Netherlands
 */
package nl.vpro.domain.shared.bind;

import javax.xml.bind.annotation.adapters.XmlAdapter;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.Duration;
import java.util.*;

public class DateToDuration extends XmlAdapter<Duration, Date> {

    protected Calendar normalizeCalendar() {
        Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("UTC"), Locale.US);
        return cal;
    }

    @Override
    public Duration marshal(Date date) throws Exception {
        DatatypeFactory datatypeFactory = DatatypeFactory.newInstance();
        Duration dur;
        if (date.getTime() < 30l * 24 * 60 * 60 * 1000) {
            dur = datatypeFactory.newDurationDayTime(date.getTime());
        } else {
            dur = datatypeFactory.newDuration(date.getTime());

        }

        dur.normalizeWith(normalizeCalendar());
        return dur;
    }

    @Override
    public Date unmarshal(Duration duration) throws Exception {
        Date date = new Date(0);
        duration.normalizeWith(normalizeCalendar());
        duration.addTo(date);
        return date;
    }
}
