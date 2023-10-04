/*
 * Copyright (C) 2011 All rights reserved
 * VPRO The Netherlands
 */
package nl.vpro.validation;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

import nl.vpro.util.TextUtil;

public class NoHtmlValidator implements ConstraintValidator<NoHtml, Object> {


    @Override
    public boolean isValid(Object value, ConstraintValidatorContext constraintValidatorContext) {
        if(value == null) {
            return true;
        }

        if(value instanceof String) {
            return TextUtil.isValid((String)value);
        }

        if(value instanceof String[]) {
            for(String item : (String[])value) {
                if(item != null && !TextUtil.isValid(item)) {
                    return false;
                }
            }
        } else if(value instanceof Iterable<?> iterable) {
            for(Object item : iterable) {
                if(item != null) {
                    if(!(item instanceof String)) {
                        throw new UnsupportedOperationException("NoHtml validation only supports collections of strings. Got an " + item.getClass().getSimpleName());
                    }

                    if(!TextUtil.isValid((String)item)) {
                        return false;
                    }
                }
            }
        }

        return true;
    }
}
