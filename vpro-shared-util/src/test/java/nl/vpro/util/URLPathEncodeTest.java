package nl.vpro.util;

import org.junit.Test;

import static org.assertj.core.api.Java6Assertions.assertThat;

/**
 * @author Michiel Meeuwissen
 * @since ...
 */
public class URLPathEncodeTest {
    @Test
    public void encode() throws Exception {

        assertThat(URLPathEncode.encode("test bla")).isEqualToIgnoringCase("test+bla");
        assertThat(URLPathEncode.encode("test&bla")).isEqualToIgnoringCase("test%26bla");
        assertThat(URLPathEncode.encode("test/bla")).isEqualToIgnoringCase("test%2Fbla");

    }

    @Test
    public void encodePath() throws Exception {
        assertThat(URLPathEncode.encodePath("test/bla/1234")).isEqualToIgnoringCase("test/bla/1234");

    }
}
