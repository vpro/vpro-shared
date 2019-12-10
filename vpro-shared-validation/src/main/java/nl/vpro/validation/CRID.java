/*
 * Copyright (C) 2011 All rights reserved
 * VPRO The Netherlands
 */
package nl.vpro.validation;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import javax.validation.Constraint;
import javax.validation.Payload;

import static java.lang.annotation.ElementType.*;
import static java.lang.annotation.RetentionPolicy.RUNTIME;


/**
 * A validator for Strings representing a crid URL.
 *
 * You may just as wel use
 */
@Target({METHOD, FIELD, PARAMETER, TYPE_USE})
@Retention(RUNTIME)
@Constraint(validatedBy = CRIDValidator.class)
@Documented
public @interface CRID {
    String message() default "{nl.vpro.constraints.crid}";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
