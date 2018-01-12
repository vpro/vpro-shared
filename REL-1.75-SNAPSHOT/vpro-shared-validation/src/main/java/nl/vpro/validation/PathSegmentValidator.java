/*
 * Copyright (C) 2014 All rights reserved
 * VPRO The Netherlands
 */
package nl.vpro.validation;

import java.io.UnsupportedEncodingException;
import java.net.URISyntaxException;
import java.net.URLDecoder;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

/**
 * @author rico
 */
public class PathSegmentValidator implements ConstraintValidator<PathSegment, String> {
    @Override
    public void initialize(PathSegment constraintAnnotation) {
    }

    @Override
    public boolean isValid(String pathSegment, ConstraintValidatorContext context) {
        return isValid(pathSegment);
    }
    protected static boolean isValid(String pathSegment) {
        if (pathSegment == null || pathSegment.length() == 0) {
            return false;
        }


        java.net.URI url;
        try {
            url = new java.net.URI(pathSegment);
        } catch (URISyntaxException e) {
            return false;
        }

        try {
            return URLDecoder.decode(pathSegment, "UTF-8").equals(url.getPath());
        } catch (UnsupportedEncodingException e) {
            return false;
        }
    }
}
