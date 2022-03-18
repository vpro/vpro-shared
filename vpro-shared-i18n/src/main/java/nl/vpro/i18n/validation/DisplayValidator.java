/*
 * Copyright (C) 2011 All rights reserved
 * VPRO The Netherlands
 */
package nl.vpro.i18n.validation;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

import nl.vpro.i18n.Displayable;

public class DisplayValidator implements ConstraintValidator<Display, Displayable> {

    @Override
    public void initialize(Display constraintAnnotation) {
    }

    @Override
    public boolean isValid(Displayable value, ConstraintValidatorContext constraintValidatorContext) {
        if(value == null) {
            return true;
        }

        return value.display();
    }
}
