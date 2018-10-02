package nl.vpro.validation;

import lombok.extern.slf4j.Slf4j;

import java.net.URISyntaxException;

import javax.validation.Validation;
import javax.validation.Validator;
import javax.validation.ValidatorFactory;

import org.junit.Test;

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

    public static class Lenient {
        @URI(lenient = true, mustHaveScheme = false, minHostParts = 3)
        String url;

        public Lenient(String url) {
            this.url = url;
        }
    }

    ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
    Validator validator = factory.getValidator();



    @Test
    public void invalidUrl() {
        A a = new A("http://www.spunk.nl/article/6127/waarom-praten-we-over-sletvrees?fb_action_ids=10202483411149472%2C10202477465200827%2C10202476551657989&fb_action_types=og.likes&fb_source=other_multiline&action_object_map={%2210202483411149472%22%3A513557392073931%2C%2210202477465200827%22%3A734595553235765%2C%2210202476551657989%22%3A537543956332495}&action_type_map={%2210202483411149472%22%3A%22og.likes%22%2C%2210202477465200827%22%3A%22og.likes%22%2C%2210202476551657989%22%3A%22og.likes%22}&action_ref_map=[]");


        assertThat( validator.validate(a)).hasSize(1);

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


}
