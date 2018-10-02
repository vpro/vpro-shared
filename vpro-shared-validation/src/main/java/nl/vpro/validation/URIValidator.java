/*
 * Copyright (C) 2011 All rights reserved
 * VPRO The Netherlands
 */
package nl.vpro.validation;

import lombok.extern.slf4j.Slf4j;

import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.util.Arrays;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

import org.apache.commons.lang3.StringUtils;

@Slf4j
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
            if (annotation.minHostParts() > 0) {
                if (uri.getHost() == null || uri.getHost().split("\\.").length < annotation.minHostParts()) {
                    log.debug("Too few host parts");
                    return false;
                }
            }
        } catch(URISyntaxException e) {
            log.debug("{}:{}",  value, e.getMessage());
            if (this.annotation.lenient()) {
                try {
                    java.net.URL url = new java.net.URL(value);
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
                    if (annotation.minHostParts() > 0) {
                        if (url.getHost() == null || url.getHost().split("\\.").length < annotation.minHostParts()) {
                            log.debug("Too few host parts");
                            return false;
                        }
                    }
                    return true;
                } catch (MalformedURLException e1) {
                    log.debug("{}:{}",  value, e.getMessage());
                    return false;
                }
            } else {
                return false;
            }
        }

        return true;
    }
}
