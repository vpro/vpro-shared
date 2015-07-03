/*
 * Copyright (C) 2014 All rights reserved
 * VPRO The Netherlands
 */
package nl.vpro.resteasy;

import java.lang.annotation.Annotation;
import java.util.Calendar;
import java.util.Date;

import org.jboss.resteasy.spi.StringParameterUnmarshaller;

/**
 * @author  rico
 */
public class IS08601Formatter implements StringParameterUnmarshaller<Date> {
    @Override
    public void setAnnotations(Annotation[] annotations) {
    }

    @Override
    public Date fromString(String str) {
        Calendar cal = javax.xml.bind.DatatypeConverter.parseDateTime(str);
        if (cal != null) {
            return cal.getTime();
        }
        return null;
    }
}
