package nl.vpro.util;

import java.util.Locale;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;


/**
 * @author Michiel Meeuwissen
 * @since ...
 */
public class MultiLanguageStringTest {

    @Test
    public void test() {
        MultiLanguageString string = MultiLanguageString.builder()
            .nl("Hoi")
            .en("Hello")
            .in("eo").is("Saluton")
            .build();

        assertThat(string.toString()).isEqualTo("Hoi");
        assertThat(string.get(new Locale("en"))).isEqualTo("Hello");
        assertThat(string.get(new Locale("eo"))).isEqualTo("Saluton");

    }

}
