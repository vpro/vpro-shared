package nl.vpro.util;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Michiel Meeuwissen
 * @since ...
 */
public class URLPathEncodeTest {
    @Test
    public void encode() {

        assertThat(URLPathEncode.encode("test bla")).isEqualToIgnoringCase("test+bla");
        assertThat(URLPathEncode.encode("test&bla")).isEqualToIgnoringCase("test%26bla");
        assertThat(URLPathEncode.encode("test/bla")).isEqualToIgnoringCase("test%2Fbla");

    }

    @Test
    public void encodePath() {
        assertThat(URLPathEncode.encodePath("test/bla/1234/jcr:node/@vpro/ /ĥ/")).isEqualToIgnoringCase("test/bla/1234/jcr:node/@vpro/+/%I5/");

        assertThat(URLPathEncode.encodePath("s1400</s3000>")).isEqualTo("s1400%3C/s3000%3E");
    }

}
