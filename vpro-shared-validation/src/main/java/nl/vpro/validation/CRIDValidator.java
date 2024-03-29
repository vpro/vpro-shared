/*
 * Copyright (C) 2011 All rights reserved
 * VPRO The Netherlands
 */
package nl.vpro.validation;

import java.util.regex.Pattern;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class CRIDValidator implements ConstraintValidator<CRID, String> {
    public static final String  REGEXP  = "crid://.*/.*";
    public static final Pattern PATTERN = Pattern.compile(REGEXP, Pattern.CASE_INSENSITIVE);



    @Override
    public boolean isValid(String value, ConstraintValidatorContext constraintValidatorContext) {
        return PATTERN.matcher(value).matches();
    }
}
