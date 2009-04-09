/**
 * Copyright (C) 2008 All rights reserved
 * VPRO The Netherlands
 */
package nl.vpro.domain.shared.bind;

import javax.xml.bind.annotation.adapters.XmlAdapter;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.Duration;
import java.util.Date;

public class DateToDuration extends XmlAdapter<Duration, Date> {

    @Override
    public Duration marshal(Date date) throws Exception {
        DatatypeFactory datatypeFactory = DatatypeFactory.newInstance();
        return datatypeFactory.newDuration(date.getTime());
    }

    @Override
    public Date unmarshal(Duration duration) throws Exception {
        Date date = new Date(0);
        duration.addTo(date);
        return date;
    }
}