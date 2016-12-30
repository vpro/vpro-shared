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
@StringParameterUnmarshallerBinder(IS08601Formatter.class)
public @interface ISO8601Format {
}
