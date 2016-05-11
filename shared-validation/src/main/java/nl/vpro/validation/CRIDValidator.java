/**
 * Copyright (C) 2011 All rights reserved
 * VPRO The Netherlands
 */
package nl.vpro.validation;

import java.util.regex.Pattern;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

public class CRIDValidator implements ConstraintValidator<CRID, String> {

    private static Pattern CRID_PATTERN = Pattern.compile("(?i)crid://.*/.*");

    @Override
    public void initialize(CRID crid) {
    }

    @Override
    public boolean isValid(String value, ConstraintValidatorContext constraintValidatorContext) {
        return CRID_PATTERN.matcher(value).matches();
    }
}
