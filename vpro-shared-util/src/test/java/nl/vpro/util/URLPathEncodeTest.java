package nl.vpro.util;


import lombok.ToString;

import java.io.UnsupportedEncodingException;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.stream.Stream;

import org.apache.commons.httpclient.URIException;
import org.apache.commons.httpclient.util.URIUtil;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Michiel Meeuwissen
 * @since ...
 */
public class URLPathEncodeTest {


    public static Stream<Case> cases() {
        return Stream.of(
            new Case("a",   "a",  "a"),
            new Case("foo bar",   "foo+bar", "foo+bar", "foo%20bar", "foo+bar"),
            new Case("ë", "%C3%AB", "%C3%AB", "%C3%AB"),
            new Case("test/bla", "test%2Fbla", "test/bla", "test/bla"),
            new Case("test/bla/1234/jcr:node/@vpro/ /ĥ/", "test%2Fbla%2F1234%2Fjcr%3Anode%2F%40vpro%2F+%2F%C4%A5%2F",
                "test%2Fbla%2F1234%2Fjcr:node%2F@vpro%2F+%2F%C4%A5%2F",
                "test/bla/1234/jcr:node/@vpro/%20/%C4%A5/",
                "test/bla/1234/jcr:node/@vpro/+/%C4%A5/"
            )
        );
    }


    @ParameterizedTest
    @MethodSource("cases")
    public void encodePath(Case c) {
        assertThat(URLPathEncode.encode(c.in)).isEqualTo(c.encodedVariant);
        assertThat(URLPathEncode.encodePath(c.in)).isEqualTo(c.pathEncodedVariant);
        assertThat(URLPathEncode.encodePath(c.in, false)).isEqualTo(c.pathEncoded);
    }

    @ParameterizedTest
    @MethodSource("cases")
    public void encodePathUrlEncoder(Case c) throws UnsupportedEncodingException {
        assertThat(URLEncoder.encode(c.in, StandardCharsets.UTF_8.name())).isEqualTo(c.encoded);
    }

    @ParameterizedTest
    @MethodSource("cases")
    public void encodePathHttpClient(Case c) throws URIException {
        assertThat(URIUtil.encodePath(c.in)).isEqualTo(c.pathEncoded);
        assertThat(URIUtil.encode(c.in, null)).isEqualTo(c.encoded);
    }

    @ParameterizedTest
    @MethodSource("cases")
    public void encodePathUri(Case c) throws URISyntaxException {
        assertThat(new URI(null, null, c.in, null).toASCIIString()).isEqualTo(c.pathEncoded);
    }

    @ParameterizedTest
    @MethodSource("cases")
    public void decode(Case c) {
        assertThat(URLPathEncode.decode(c.encoded)).isEqualTo(c.in);
        assertThat(URLPathEncode.decode(c.encodedVariant)).isEqualTo(c.in);
        assertThat(URLPathEncode.decode(c.pathEncoded)).isEqualTo(c.in);
        assertThat(URLPathEncode.decode(c.pathEncodedVariant)).isEqualTo(c.in);
    }

     @ParameterizedTest
    @MethodSource("cases")
    public void decodeHttpClient(Case c) throws URIException {
        assertThat(URIUtil.decode(c.encoded)).isEqualTo(c.in);
        assertThat(URIUtil.decode(c.encodedVariant)).isEqualTo(c.in);
        assertThat(URIUtil.decode(c.pathEncoded)).isEqualTo(c.in);
        assertThat(URIUtil.decode(c.pathEncodedVariant)).isEqualTo(c.in);
    }

    @Test
    public void encode() {

        assertThat(URLPathEncode.encode("test bla")).isEqualToIgnoringCase("test+bla");
        assertThat(URLPathEncode.encode("test&bla")).isEqualToIgnoringCase("test%26bla");
        assertThat(URLPathEncode.encode("test/bla")).isEqualToIgnoringCase("test%2Fbla");

    }

    @Test
    public void encodePath() {
        assertThat(URLPathEncode.encodePath("test/bla/1234/jcr:node/@vpro/ /ĥ/")).isEqualToIgnoringCase("test/bla/1234/jcr:node/@vpro/+/%C4%A5/");

        assertThat(URLPathEncode.encodePath("s1400</s3000>")).isEqualTo("s1400%3C/s3000%3E");
    }

    @ToString
    static class Case {
        final String in;
        final String encoded;                 // httpclient's URIUtil#encode does this

         /**
         * The variant has + for space, and doesn't esacpe a few other charachters like semicolon
         */
        final String encodedVariant;
        final String pathEncoded;             // httpclient's URIUtil#encodePath does this, URLPathEncode#encodePath(c.in, false)

        /**
         * The variant has + for space, and
         */
        final String pathEncodedVariant;      // URLPathEncode#encodePath(c.in)

        Case(String in, String encoded, String pathEncoded) {
            this(in, encoded, encoded, pathEncoded, pathEncoded);
        }

        Case(String in, String encoded, String pathEncoded, String pathEncodedVariant) {
            this(in, encoded, encoded, pathEncoded, pathEncodedVariant);
        }

        Case(String in, String encoded, String encodedVariant, String pathEncoded, String pathEncodedVariant) {
            this.in = in;
            this.encoded = encoded;
            this.encodedVariant = encodedVariant;
            this.pathEncoded = pathEncoded;
            this.pathEncodedVariant = pathEncodedVariant;
        }
    }

}
