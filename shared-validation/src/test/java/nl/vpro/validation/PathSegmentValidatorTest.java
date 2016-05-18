package nl.vpro.validation;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;


/**
 * See also https://jira.vpro.nl/browse/NPA-305
 * @author Michiel Meeuwissen
 * @since 4.4
 */
public class PathSegmentValidatorTest {


    @Test
    public void testSimple() {
        String simple = "/de-vloer-op/19-08-2005/HUMAN_20050819_vloer";

        assertThat(PathSegmentValidator.isValid(simple)).isTrue();
    }

    @Test
    public void testUrlEncode() {
       String withEncoding = "/de-vloer-op/19-08-2005/HUMAN_20050819%20vloer";

        assertThat(PathSegmentValidator.isValid(withEncoding)).isTrue();
    }

    @Test
    public void testUrlUnencode() {
        String withEncoding = "/de-vloer-op/19-08-2005/HUMAN_20050819 vloer";

        assertThat(PathSegmentValidator.isValid(withEncoding)).isTrue();
    }

}
