/*
 * Copyright (C) 2008 All rights reserved
 * VPRO The Netherlands
 */
package nl.vpro.xml.bind;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

import javax.xml.bind.annotation.adapters.XmlAdapter;

public class DateToXMLDate extends XmlAdapter<String, Date> {

    private final ThreadLocal<DateFormat> DF = ThreadLocal.withInitial(() -> new SimpleDateFormat("yyyy-MM-dd"));

    @Override
    public Date unmarshal(String date) throws Exception {
        return DF.get().parse(date);
    }

    @Override
    public String marshal(Date date) {
        return DF.get().format(date);
    }
}
