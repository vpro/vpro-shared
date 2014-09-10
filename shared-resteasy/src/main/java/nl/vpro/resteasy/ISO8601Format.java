/*
 * Copyright (C) 2014 All rights reserved
 * VPRO The Netherlands
 */
package nl.vpro.resteasy;

import org.jboss.resteasy.annotations.StringParameterUnmarshallerBinder;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * User: rico
 * Date: 06/02/2014
 */
@Retention(RetentionPolicy.RUNTIME)
@StringParameterUnmarshallerBinder(IS08601Formatter.class)
public @interface ISO8601Format {
}
