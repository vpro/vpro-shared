/**
 * Copyright (C) 2011 All rights reserved
 * VPRO The Netherlands
 */
package nl.vpro.validation;

import java.util.Collection;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

import nl.vpro.util.TextUtil;

public class NoHtmlListValidator implements ConstraintValidator<NoHtmlList, Collection<String>> {

    @Override
    public void initialize(NoHtmlList constraintAnnotation) {
    }

    @Override
    public boolean isValid(Collection<String> value, ConstraintValidatorContext constraintValidatorContext) {
        if(value == null) {
            return true;
        }
        for (String v : value) {
            if (! TextUtil.isValid(v)) {
                return false;
            }
        }
        return true;
    }
}
