/*
 * Copyright (C) 2013 All rights reserved
 * VPRO The Netherlands
 */
package nl.vpro.validation;

import org.junit.Test;

import nl.vpro.validation.NoHtmlValidator;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * @author Roelof Jan Koekoek
 */
public class NoHtmlValidatorTest {
    private NoHtmlValidator validator = new NoHtmlValidator();

    @Test
    public void testIsValid() throws Exception {
        assertTrue(validator.isValid("some text", null));
    }

    @Test
    public void testIsValidShouldFailOnTags() throws Exception {
        assertFalse(validator.isValid("some <text>", null));
    }

    @Test
    public void testIsValidShouldFailOnEntityNames() throws Exception {
        assertFalse(validator.isValid("some &amp; text", null));
    }

    @Test
    public void testIsValidShouldFailOnEntityNumbers() throws Exception {
        assertFalse(validator.isValid("some&#20;text", null));
    }
}
