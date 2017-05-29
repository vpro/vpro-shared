/*
 * Copyright (C) 2013 All rights reserved
 * VPRO The Netherlands
 */
package nl.vpro.xml.bind;

import java.util.Date;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.bind.annotation.adapters.XmlAdapter;

/**
 * @author Roelof Jan Koekoek
 * @since 2.1
 * @deprecated
 */
public class SecondsToDateAdapter extends XmlAdapter<String, Date> {

    private final static Pattern PATTERN = Pattern.compile("(\\d+)(?:\\.(\\d{1,3})(?:\\d*))?");

    @Override
    public Date unmarshal(String seconds) throws Exception {
        Matcher matcher = PATTERN.matcher(seconds);
        if(matcher.find()) {
            long result = Long.valueOf(matcher.group(1)) * 1000;

            if(matcher.group(2) != null) {
                Long decimalPart = Long.valueOf(matcher.group(2));
                result += decimalPart < 10 ? decimalPart * 100 : decimalPart < 100 ? decimalPart * 10 : decimalPart;
            }

            return new Date(result);
        }

        throw new RuntimeException("Can't parse seconds from duration: " + seconds);
    }

    @Override
    public String marshal(Date v) throws Exception {
        throw new UnsupportedOperationException("Not implemented");
    }
}
