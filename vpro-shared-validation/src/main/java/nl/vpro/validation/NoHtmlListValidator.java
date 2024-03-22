/*
 * Copyright (C) 2011 All rights reserved
 * VPRO The Netherlands
 */
package nl.vpro.validation;

import java.util.Collection;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import nl.vpro.util.TextUtil;

@Deprecated
public class NoHtmlListValidator implements ConstraintValidator<NoHtmlList, Collection<String>> {

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
