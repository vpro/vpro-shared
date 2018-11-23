package nl.vpro.util;

import org.junit.Test;

import static nl.vpro.util.Version.parseIntegers;
import static org.assertj.core.api.Assertions.assertThat;


/**
 * @author Michiel Meeuwissen
 * @since ...
 */
public class VersionTest {

    @Test
    public void compareTo() {

        assertThat(parseIntegers("1.2.3")).isLessThan(parseIntegers("1.2.4"));
        assertThat(parseIntegers("1.2.3")).isGreaterThan(parseIntegers("1.2"));
        assertThat(parseIntegers("1.2.3").isAfter(1, 2)).isTrue();
        assertThat(parseIntegers("2.2.3").isAfter(1, 2, 5, 4)).isTrue();
        assertThat(parseIntegers("2.2.3").isNotBefore(2, 2, 3)).isTrue();
        assertThat(parseIntegers("2.2.3").isNotBefore(2, 2, 4)).isFalse();




    }

}
