/*
 * Copyright (C) 2011 All rights reserved
 * VPRO The Netherlands
 */
package nl.vpro.i18n.validation;

import java.lang.annotation.*;

import javax.validation.Constraint;
import javax.validation.Payload;

import nl.vpro.i18n.Displayable;

import static java.lang.annotation.ElementType.*;
import static java.lang.annotation.RetentionPolicy.RUNTIME;


/**
 * Marks that a {@link nl.vpro.i18n.Displayable} values is invalid if its value for {@link Displayable#display()} is not true.
 */
@Target({METHOD, FIELD, ANNOTATION_TYPE, TYPE_USE})
@Retention(RUNTIME)
@Constraint(validatedBy = DisplayableValidator.class)
@Documented
public @interface MustDisplay {
    String message() default "{nl.vpro.constraints.display}";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
