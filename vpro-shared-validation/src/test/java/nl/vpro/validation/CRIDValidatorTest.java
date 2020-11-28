package nl.vpro.validation;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author Michiel Meeuwissen
 * @since 2.3
 */
public class CRIDValidatorTest {
    @Test
    public void testIsValid() {
        CRIDValidator validator = new CRIDValidator();
        assertFalse(validator.isValid("http://bla/bla", null));
        assertFalse(validator.isValid("crid://test", null));
        assertTrue(validator.isValid("crid://test/test", null));
        assertTrue(validator.isValid("CRID://test/test", null));
        assertFalse(validator.isValid(" CRID://test/test", null));

    }


}
