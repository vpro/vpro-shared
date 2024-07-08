package nl.vpro.validation;

import lombok.extern.slf4j.Slf4j;

import java.net.*;

import jakarta.validation.*;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Michiel Meeuwissen
 * @since ...
 */
@Slf4j
public class URIValidatorTest {

    public static class A {
        @URI(mustHaveScheme = true, minHostParts = 3)
        String url;

        public A(String url) {
            this.url = url;
        }
    }

    public static class AAllowEmpty {
        @URI(mustHaveScheme = true, minHostParts = 3, allowEmptyString = true)
        String url;

        public AAllowEmpty(String url) {
            this.url = url;
        }
    }

    public static class Lenient {
        @URI(lenient = true, mustHaveScheme = false, minHostParts = 3)
        String url;

        public Lenient(String url) {
            this.url = url;
        }
    }
    public static class Gtaa {
        @URI(schemes = {"http"}, mustHaveScheme = true, hosts = {"data.beeldengeluid.nl"}, patterns = {"http://data\\.beeldengeluid\\.nl/gtaa/\\d+"})
        java.net.URI uri;
        public Gtaa(String url) {
            this.uri = java.net.URI.create(url);
        }

    }

    ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
    Validator validator = factory.getValidator();

    @Test
    public void invalidUrl() {
        A a = new A("http://www.spunk.nl/article/6127/waarom-praten-we-over-sletvrees?fb_action_ids=10202483411149472%2C10202477465200827%2C10202476551657989&fb_action_types=og.likes&fb_source=other_multiline&action_object_map={%2210202483411149472%22%3A513557392073931%2C%2210202477465200827%22%3A734595553235765%2C%2210202476551657989%22%3A537543956332495}&action_type_map={%2210202483411149472%22%3A%22og.likes%22%2C%2210202477465200827%22%3A%22og.likes%22%2C%2210202476551657989%22%3A%22og.likes%22}&action_ref_map=[]");


        assertThat(validator.validate(a)).hasSize(1);
    }


    @Test
    public void invalidUrlLenient() {
        Lenient a = new Lenient("http://www.spunk.nl/article/6127/waarom-praten-we-over-sletvrees?fb_action_ids=10202483411149472%2C10202477465200827%2C10202476551657989&fb_action_types=og.likes&fb_source=other_multiline&action_object_map={%2210202483411149472%22%3A513557392073931%2C%2210202477465200827%22%3A734595553235765%2C%2210202476551657989%22%3A537543956332495}&action_type_map={%2210202483411149472%22%3A%22og.likes%22%2C%2210202477465200827%22%3A%22og.likes%22%2C%2210202476551657989%22%3A%22og.likes%22}&action_ref_map=[]");

        assertThat(validator.validate(a)).isEmpty();
    }

    @Test
    public void withoutScheme() {
        assertThat(validator.validate(new A("//www.vpro.nl"))).hasSize(1);

        assertThat(validator.validate(new Lenient("//www.vpro.nl"))).hasSize(0);
    }

    @Test
    public void minHostParts() {
        assertThat(validator.validate(new A("http://vpro.nl"))).hasSize(1);

        assertThat(validator.validate(new Lenient("http://vpro.nl?{}"))).hasSize(1);
        assertThat(validator.validate(new Lenient("http://www.vpro.nl?{}"))).hasSize(0);
    }

    @Test
    public void noHost() throws URISyntaxException {
        java.net.URI uri = new java.net.URI(null, null, "path", null);
        String url = uri.toString();
        log.info("{}", url);
        assertThat(validator.validate(new A(uri.toString()))).hasSize(1);

        assertThat(validator.validate(new Lenient("http://vpro.nl?{}"))).hasSize(1);
        assertThat(validator.validate(new Lenient("http://www.vpro.nl?{}"))).hasSize(0);
    }

    @Test
    public void emptyString() throws URISyntaxException {
        A a1 = new A("");
        A a2 = new A(null);

        assertThat(validator.validate(a1)).hasSize(1);
        assertThat(validator.validate(a2)).isEmpty();

        AAllowEmpty ae1 = new AAllowEmpty("");
        AAllowEmpty ae2 = new AAllowEmpty(null);

        assertThat(validator.validate(ae1)).isEmpty();
        assertThat(validator.validate(ae2)).isEmpty();
    }


    @ParameterizedTest
    @CsvSource(textBlock = """
        https://radioimages.npox.nl//carl_johan_1200x675[1276847].jpg, false, true
        https://radioimages.npox.nl/carl_johan_1200x675[1276847].jpg, false, true
        """
    )
    public void validInvalid(String uri, boolean valid, boolean lenientlyValid) {

        A astrict = new A(uri);
        assertThat(validator.validate(astrict)).hasSize(valid ? 0 : 1);


        Lenient alenient = new Lenient(uri);
        assertThat(validator.validate(alenient)).hasSize(lenientlyValid ? 0 : 1);


    }


    @ParameterizedTest
    @CsvSource(textBlock = """
        http://data.beeldengeluid.nl/gtaa/241892, true
        https://data.beeldengeluid.nl/gtaa/241892, false
        http://dataa.beeldengeluid.nl/gtaa/241892, false
        http://data.beeldengeluid.nl/gtab/241892, false
        http://data.beeldengeluid.nl/gtaa/aaaa, false
        //data.beeldengeluid.nl/gtaa/1234, false
        """
    )
    public void gtaa(String uri, boolean valid) throws URISyntaxException, NoSuchFieldException, MalformedURLException {

        Gtaa a1 = new Gtaa(uri);
        assertThat(validator.validate(a1)).hasSize(valid ? 0 : 1);
        URIValidator uriValidator = new URIValidator();
        uriValidator.initialize(a1.getClass().getDeclaredField("uri").getAnnotation(URI.class));


        assertThat(uriValidator.validateCharSequence(uri)).isEqualTo(valid);

        try {
            assertThat(uriValidator.validateURL(new URL(uri))).isEqualTo(valid);
        } catch (MalformedURLException mue) {
            assertThat(valid).isFalse();
        }
        assertThat(uriValidator.validateURI(java.net.URI.create(uri))).isEqualTo(valid);
    }

}
