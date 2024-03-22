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

@Target({METHOD, FIELD, ANNOTATION_TYPE, TYPE_USE})
@Retention(RUNTIME)
@Constraint(validatedBy = NoHtmlValidator.class)
@Documented
public @interface NoHtml {
    String message() default "{nl.vpro.constraints.nohtml}";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};

    /**
     * If 'aggressive' no html is set, then all kind of even remotely like html are rejected. This is the original behaviour.
     * <p>
     * When this is false, the checking will be more subtle, and things like 'email: <foo@gmail.com>' will be accepted. Basically text is only invalid, if jsoup suceeeds finding html tags or entities.
     * @since 4.3
     */
    boolean aggressive() default true;
}
