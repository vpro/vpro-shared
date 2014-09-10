/*
 * Copyright (C) 2014 All rights reserved
 * VPRO The Netherlands
 */
package nl.vpro.resteasy;

import org.jboss.resteasy.spi.StringParameterUnmarshaller;
import org.jboss.resteasy.util.FindAnnotation;

import java.lang.annotation.Annotation;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * User: rico
 * Date: 06/02/2014
 */
public class DateFormatter implements StringParameterUnmarshaller<Date> {
    private SimpleDateFormat formatter;

    @Override
    public void setAnnotations(Annotation[] annotations) {
        DateFormat format = FindAnnotation.findAnnotation(annotations, DateFormat.class);
        formatter = new SimpleDateFormat(format.value());
    }

    @Override
    public Date fromString(String str) {
        try {
            return formatter.parse(str);
        } catch (ParseException e) {
            throw new RuntimeException(e);
        }
    }
}


