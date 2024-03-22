package nl.vpro.validation;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import javax.validation.Constraint;
import javax.validation.Payload;

import static java.lang.annotation.ElementType.ANNOTATION_TYPE;
import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * @author Michiel Meeuwissen
 * @since 3.2
 * @deprecated Just put @NoHtml on the list entries itself (List<@NoHtml String>)
 */


@Target({METHOD, FIELD, ANNOTATION_TYPE})
@Retention(RUNTIME)
@Constraint(validatedBy = NoHtmlListValidator.class)
@Documented
@Deprecated
public @interface NoHtmlList {
    String message() default "{nl.vpro.constraints.nohtml}";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}

