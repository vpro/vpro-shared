/**
 * Copyright (C) 2008 All rights reserved
 * VPRO The Netherlands
 */
package nl.vpro.xml.bind;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.function.Supplier;

import javax.xml.bind.annotation.adapters.XmlAdapter;

public class DateTimeToXMLDate extends XmlAdapter<String, Date> {

    ThreadLocal<DateFormat> DF = ThreadLocal.withInitial(new Supplier<DateFormat>() {
        @Override
        public DateFormat get() {
            return new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

        }
    });

    @Override
    public Date unmarshal(String date) throws Exception {
        return DF.get().parse(date);
    }

    @Override
    public String marshal(Date date) throws Exception {
        return DF.get().format(date);
    }
}
