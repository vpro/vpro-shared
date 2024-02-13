/*
 * Copyright (C) 2014 All rights reserved
 * VPRO The Netherlands
 */
package nl.vpro.validation;

import java.net.URISyntaxException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

/**
 * @author rico
 */
public class PathSegmentValidator implements ConstraintValidator<PathSegment, String> {


    @Override
    public boolean isValid(String pathSegment, ConstraintValidatorContext context) {
        return isValid(pathSegment);
    }
    protected static boolean isValid(String pathSegment) {
        if (pathSegment == null || pathSegment.isEmpty()) {
            return false;
        }


        java.net.URI url;
        try {
            url = new java.net.URI(pathSegment);
        } catch (URISyntaxException e) {
            return false;
        }

        return URLDecoder.decode(pathSegment, StandardCharsets.UTF_8).equals(url.getPath());
    }
}
