/*
 * Copyright (C) 2011 All rights reserved
 * VPRO The Netherlands
 */
package nl.vpro.validation;

import lombok.extern.slf4j.Slf4j;

import java.net.*;
import java.util.Arrays;
import java.util.regex.Pattern;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

import org.apache.commons.lang3.StringUtils;

@Slf4j
public class URIValidator implements ConstraintValidator<URI, Object> {

    URI annotation;

    @Override
    public void initialize(URI uri) {
        this.annotation = uri;
    }

    @Override
    public boolean isValid(Object value, ConstraintValidatorContext constraintValidatorContext) {
        if(value == null) {
            return true;
        }

        if (value instanceof CharSequence) {
            return validateCharSequence((CharSequence) value);
        }
        if (value instanceof java.net.URI) {
            return validateURI((java.net.URI) value);
        }
        if (value instanceof URL) {
            return validateURL((URL) value);
        }
        log.debug("Type unrecognized");
        return false;

    }





    boolean validateCharSequence(CharSequence value) {
        if (StringUtils.isEmpty(value) && annotation.allowEmptyString()) {
            return true;
        }

        try {
            java.net.URI uri = new java.net.URI(value.toString());
            return validateURI(uri);
        } catch(URISyntaxException e) {
            log.debug("{}:{}",  value, e.getMessage());
            if (this.annotation.lenient()) {
                try {
                    java.net.URL url = new java.net.URL(value.toString());
                    return validateURL(url);
                } catch (MalformedURLException e1) {
                    log.debug("{}:{}",  value, e.getMessage());
                    return false;
                }
            } else {
                return false;
            }
        }
    }

    boolean validateURI(java.net.URI uri) {
        if (annotation.mustHaveScheme()) {
            if (StringUtils.isEmpty(uri.getScheme())) {
                log.debug("Scheme empty");
                return false;
            }
        }
        if (annotation.schemes().length > 0) {
            if (!Arrays.asList(annotation.schemes()).contains(uri.getScheme())) {
                log.debug("Scheme not one of {}", Arrays.asList(annotation.schemes()));
                return false;
            }
        }
        if (annotation.hosts().length > 0) {
            if (! Arrays.asList(annotation.hosts()).contains(uri.getHost())) {
                log.debug("Host not one of {}", Arrays.asList(annotation.hosts()));
                return false;
            }
        }
        if (annotation.minHostParts() > 0) {
            if (uri.getHost() == null || uri.getHost().split("\\.").length < annotation.minHostParts()) {
                log.debug("Too few host parts");
                return false;
            }
        }
        for (String p : annotation.patterns()) {
            if (!Pattern.compile(p).matcher(uri.toString()).matches()) {
                return false;
            }
        }
        return true;
    }

    boolean validateURL(URL url) {
        if (annotation.mustHaveScheme()) {
            if (StringUtils.isEmpty(url.getProtocol())) {
                log.debug("Scheme empty");
                return false;
            }
        }
        if (annotation.schemes().length > 0) {
            if (! Arrays.asList(annotation.schemes()).contains(url.getProtocol())) {
                log.debug("Scheme not one of {}", Arrays.asList(annotation.schemes()));
                return false;
            }
        }

        if (annotation.hosts().length > 0) {
            if (! Arrays.asList(annotation.hosts()).contains(url.getHost())) {
                log.debug("Host not one of {}", Arrays.asList(annotation.hosts()));
                return false;
            }
        }
        if (annotation.minHostParts() > 0) {
            if (url.getHost() == null || url.getHost().split("\\.").length < annotation.minHostParts()) {
                log.debug("Too few host parts");
                return false;
            }
        }

        for (String p : annotation.patterns()) {
            if (!Pattern.compile(p).matcher(url.toString()).matches()) {
                return false;
            }
        }
        return true;
    }
}
