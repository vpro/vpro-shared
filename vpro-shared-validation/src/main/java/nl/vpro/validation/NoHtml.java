/*
 * Copyright (C) 2011 All rights reserved
 * VPRO The Netherlands
 */
package nl.vpro.validation;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import static java.lang.annotation.ElementType.*;
import static java.lang.annotation.RetentionPolicy.RUNTIME;


/**
 * Marks a field as not allowed to contain HTML. Implemented in {@link NoHtmlValidator}, which deletages mustly to {@link nl.vpro.util.TextUtil#isValid(String, boolean)}.
 */
@Target({METHOD, FIELD, ANNOTATION_TYPE, TYPE_USE})
@Retention(RUNTIME)
@Constraint(validatedBy = NoHtmlValidator.class)
@Documented
public @interface NoHtml {
    String message() default "{nl.vpro.constraints.nohtml}";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};

    /**
     * If 'aggressive' no HTML is set, then all kind of even remotely like HTML are rejected. This is the original behavior.
     * <p>
     * When this is false, the checking will be more subtle, and things like 'email: &lt;foo@gmail.com&gt;' will be accepted. Basically, a text is only invalid if jsoup succeeds in finding HTML tags or entities.
     * @since 4.3
     */
    boolean aggressive() default true;
}
