/*
 * Copyright (C) 2008 All rights reserved
 * VPRO The Netherlands
 */
package nl.vpro.xml.bind;

import java.util.Date;

import jakarta.xml.bind.annotation.adapters.XmlAdapter;

import org.apache.commons.lang3.time.FastDateFormat;

public class DateTimeToXMLDate extends XmlAdapter<String, Date> {

    private static final FastDateFormat DF = FastDateFormat.getInstance("yyyy-MM-dd HH:mm:ss");

    @Override
    public Date unmarshal(String date) throws Exception {
        return DF.parse(date);
    }

    @Override
    public String marshal(Date date) {
        return DF.format(date);
    }
}
