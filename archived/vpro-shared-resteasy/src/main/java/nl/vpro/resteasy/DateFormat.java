/*
 * Copyright (C) 2014 All rights reserved
 * VPRO The Netherlands
 */
package nl.vpro.resteasy;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

import org.jboss.resteasy.annotations.StringParameterUnmarshallerBinder;

/**
 * @author rico
 */
@Retention(RetentionPolicy.RUNTIME)
@StringParameterUnmarshallerBinder(DateFormatter.class)
public @interface DateFormat {
    String value();
}
