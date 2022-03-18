/*
 * Copyright (C) 2011 All rights reserved
 * VPRO The Netherlands
 */
package nl.vpro.i18n.validation;

import java.lang.annotation.*;

import javax.validation.Constraint;
import javax.validation.Payload;

import static java.lang.annotation.ElementType.*;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

@Target({METHOD, FIELD, ANNOTATION_TYPE, TYPE_USE})
@Retention(RUNTIME)
@Constraint(validatedBy = DisplayValidator.class)
@Documented
public @interface Display {
    String message() default "{nl.vpro.constraints.display}";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
