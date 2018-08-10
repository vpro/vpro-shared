/*
 * Copyright (C) 2011 All rights reserved
 * VPRO The Netherlands
 */
package nl.vpro.validation;

import java.net.URISyntaxException;
import java.util.Arrays;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

import org.apache.commons.lang3.StringUtils;

public class URIValidator implements ConstraintValidator<URI, String> {

    URI annotation;

    @Override
    public void initialize(URI uri) {
        this.annotation = uri;
    }

    @Override
    public boolean isValid(String value, ConstraintValidatorContext constraintValidatorContext) {
        if(value == null) {
            return true;
        }

        try {
            java.net.URI uri = new java.net.URI(value);
            if (annotation.mustHaveScheme()) {
                return StringUtils.isNotEmpty(uri.getScheme());
            }
            if (annotation.schemes().length > 0) {
                return Arrays.asList(annotation.schemes()).contains(uri.getScheme());
            }
            if (annotation.minHostParts() > 0) {
                return uri.getHost() != null && uri.getHost().split("\\.").length >= annotation.minHostParts();
            }
        } catch(URISyntaxException e) {
            return false;
        }

        return true;
    }
}
