/*
 * Copyright (C) 2011 All rights reserved
 * VPRO The Netherlands
 */
package nl.vpro.validation;

import java.util.regex.Pattern;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

public class CRIDValidator implements ConstraintValidator<CRID, String> {

    public static final Pattern PATTERN = Pattern.compile("crid://.*/.*", Pattern.CASE_INSENSITIVE);

    @Override
    public void initialize(CRID crid) {
    }

    @Override
    public boolean isValid(String value, ConstraintValidatorContext constraintValidatorContext) {
        return PATTERN.matcher(value).matches();
    }
}
