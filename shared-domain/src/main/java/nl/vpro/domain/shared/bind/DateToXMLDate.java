/**
 * Copyright (C) 2008 All rights reserved
 * VPRO The Netherlands
 */
package nl.vpro.domain.shared.bind;

import javax.xml.bind.annotation.adapters.XmlAdapter;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

public class DateToXMLDate extends XmlAdapter<String, Date> {

    DateFormat df = new SimpleDateFormat("yyyy-MM-dd");

    public Date unmarshal(String date) throws Exception {
        return df.parse(date);
    }

    public String marshal(Date date) throws Exception {
        return df.format(date);
    }
}