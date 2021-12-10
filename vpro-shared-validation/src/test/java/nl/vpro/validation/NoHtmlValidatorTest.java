/*
 * Copyright (C) 2013 All rights reserved
 * VPRO The Netherlands
 */
package nl.vpro.validation;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author Roelof Jan Koekoek
 */
public class NoHtmlValidatorTest {
    private final NoHtmlValidator validator = new NoHtmlValidator();

    @Test
    public void testIsValid() {
        assertTrue(validator.isValid("some text", null));
    }

    @Test
    public void testIsValidShouldFailOnTags() {
        assertFalse(validator.isValid("some <text>", null));
    }

    @Test
    public void testIsValidShouldFailOnEntityNames() {
        assertFalse(validator.isValid("some &amp; text", null));
    }

    @Test
    public void testIsValidShouldFailOnEntityNumbers() {
        assertFalse(validator.isValid("some&#20;text", null));
    }
}
