/*
 * Copyright (C) 2011 All rights reserved
 * VPRO The Netherlands
 */
package nl.vpro.validation;

import java.lang.annotation.*;

import javax.validation.Constraint;
import javax.validation.Payload;

import static java.lang.annotation.ElementType.*;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * Marks a {@link String} as representing a valid URI or URL. The basic check is whether
 * {@code
 * new URI(<String>)
 * }
 * throws an exception
 */

@Constraint(validatedBy = URIValidator.class)
@Documented
@Target({ METHOD, FIELD, ANNOTATION_TYPE, CONSTRUCTOR, PARAMETER, TYPE_USE })
@Retention(RUNTIME)
@Repeatable(URI.List.class)
public @interface URI {
    String message() default "{nl.vpro.constraints.URI}";

    /**
     * If true the URI must have a scheme.
     */
    boolean mustHaveScheme() default false;

     /**
      * The number of parts (when the string is separated by a dot) the URI must have.
      * E.g. if this is 2, then the url {@code http://vpro/foobar} will not be valid.
      * (because you'd expect at least something like {@code http://vpro.nl/foobar}
      */
    int  minHostParts() default 0;


    /**
     * If set, the acceptable schemes. E.g. {http, https}
     */
    String[] schemes() default {};

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};

    /**
     * If the check is lenient, than also 'new URL' will be tried.
     */
    boolean lenient() default false;

    boolean allowEmptyString() default false;


	/**
	 * Defines several {@code @URI} constraints on the same element.
	 *
	 * @see URI
	 */
	@Target({ METHOD, FIELD, ANNOTATION_TYPE, CONSTRUCTOR, PARAMETER, TYPE_USE })
	@Retention(RUNTIME)
	@Documented
    @interface List {
		URI[] value();
	}
}
