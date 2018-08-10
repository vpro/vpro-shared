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

@Target({METHOD, FIELD, ANNOTATION_TYPE})
@Retention(RUNTIME)
@Constraint(validatedBy = URIValidator.class)
@Documented
public @interface URI {
    String message() default "{nl.vpro.constraints.URI}";

    boolean mustHaveScheme() default false;

    int  minHostParts() default 0;

    String[] schemes() default {};

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
