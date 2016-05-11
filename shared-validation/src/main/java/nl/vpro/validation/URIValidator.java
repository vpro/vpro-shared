/**
 * Copyright (C) 2011 All rights reserved
 * VPRO The Netherlands
 */
package nl.vpro.validation;

import java.net.URISyntaxException;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

public class URIValidator implements ConstraintValidator<URI, String> {

    @Override
    public void initialize(URI uri) {
    }

    @Override
    public boolean isValid(String value, ConstraintValidatorContext constraintValidatorContext) {
        if(value == null) {
            return true;
        }

        try {
            new java.net.URI(value);
        } catch(URISyntaxException e) {
            return false;
        }

        return true;
    }
}
