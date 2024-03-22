/*
 * Copyright (C) 2011 All rights reserved
 * VPRO The Netherlands
 */
package nl.vpro.validation;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

import nl.vpro.util.TextUtil;

public class NoHtmlValidator implements ConstraintValidator<NoHtml, Object> {

    private boolean aggressive;

    @Override
	public void initialize(NoHtml annotation) {
        aggressive = annotation.aggressive();
	}

    @Override
    public boolean isValid(Object value, ConstraintValidatorContext constraintValidatorContext) {
        if (value == null) {
            return true;
        }


        if (value instanceof CharSequence string) {
            return TextUtil.isValid(string.toString(), aggressive);
        }


        // support some iteables too.

        if (value instanceof String[] strings) {
            for(String item : strings) {
                if(item != null && !TextUtil.isValid(item, aggressive)) {
                    return false;
                }
            }
        } else if (value instanceof Iterable<?> iterable) {
            for(Object item : iterable) {
                if(item != null) {
                    if(!(item instanceof CharSequence string)) {
                        throw new UnsupportedOperationException("NoHtml validation only supports collections of strings. Got an " + item.getClass().getSimpleName());
                    }

                    if(!TextUtil.isValid(string.toString(), aggressive)) {
                        return false;
                    }
                }
            }
        }
        // all other types are considered valid

        return true;
    }
}
