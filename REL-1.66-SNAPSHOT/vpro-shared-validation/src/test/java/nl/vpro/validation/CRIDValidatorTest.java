package nl.vpro.validation;

import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * @author Michiel Meeuwissen
 * @since 2.3
 */
public class CRIDValidatorTest {
    @Test
    public void testIsValid() throws Exception {
        CRIDValidator validator = new CRIDValidator();
        assertFalse(validator.isValid("http://bla/bla", null));
        assertFalse(validator.isValid("crid://test", null));
        assertTrue(validator.isValid("crid://test/test", null));
        assertTrue(validator.isValid("CRID://test/test", null));
        assertFalse(validator.isValid(" CRID://test/test", null));

    }


}
