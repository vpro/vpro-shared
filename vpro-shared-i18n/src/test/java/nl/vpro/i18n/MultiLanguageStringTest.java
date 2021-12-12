package nl.vpro.i18n;

import java.util.Locale;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;


/**
 * @author Michiel Meeuwissen
 */
public class MultiLanguageStringTest {


    @Test
    public void test() {

        MultiLanguageString string = MultiLanguageString
            .en("Hello")
            .nl("Hoi")
            .in("eo").is("Saluton")
            .defaultLocale(new Locale("nl"))
            .build();

        assertThat(string.toString()).isEqualTo("Hoi");
        assertThat(string.get(new Locale("en"))).isEqualTo("Hello");
        assertThat(string.get(new Locale("eo"))).isEqualTo("Saluton");
        assertThat(string.get(new Locale("en", "US"))).isEqualTo("Hello");
        assertThat(string.get(new Locale("fr"))).isEqualTo("Hoi");

        assertThat(MultiLanguageString.get(string, Locale.ENGLISH)).isEqualTo("Hello");
        assertThat(MultiLanguageString.get("Hello", Locale.ENGLISH)).isEqualTo("Hello");

        assertThat(string.length()).isEqualTo(3);
        assertThat(string.charAt(1)).isEqualTo('o');
        assertThat(string.subSequence(1, 3)).isEqualTo("oi");

    }

    @Test
    public void testWithoutDefault() {
        MultiLanguageString string = MultiLanguageString
            .en("Hello")
            .nl("Hoi")
            .in("eo").is("Saluton")
            .build();
        assertThat(string.get(new Locale("fr"))).isNull();
        assertThat(string.get(new Locale("en", "US"))).isEqualTo("Hello");
    }


    @Test
    public void testWithDefaultLocale() {

        MultiLanguageString string = MultiLanguageString.builder()
            .nl("Hoi")
            .en("Hello")
            .in("eo").is("Saluton")
            .build();
        try (Locales.RestoreDefaultLocale ignore1 = Locales.with(Locales.DUTCH)) {
            try (Locales.RestoreDefaultLocale ignore2 = Locales.with(Locale.ENGLISH)) {
                assertThat(string.toString()).isEqualTo("Hello");
            }
            assertThat(string.toString()).isEqualTo("Hoi");
        }


        assertThat(string.get(new Locale("en"))).isEqualTo("Hello");
        assertThat(string.get(new Locale("eo"))).isEqualTo("Saluton");
        assertThat(string.get(new Locale("en", "US"))).isEqualTo("Hello");
    }

    @Test
    public void in() {
        MultiLanguageString string = MultiLanguageString
            .of(Locale.CHINESE, "asdfad")
            .build();
        assertThat(string.toString()).isEqualTo("asdfad");
    }

     @Test
    public void testWithSlfjArgs() {
        MultiLanguageString string = MultiLanguageString.builder()
            .nl("a {} b {}")
            .en("b {} a {}")
            .in("eo").is("c {} {}")
            .defaultLocale(new Locale("nl"))
            .slf4jArgs("A", 1)
            .build();

        assertThat(string.toString()).isEqualTo("a A b 1");
        assertThat(string.get(new Locale("en"))).isEqualTo("b A a 1");
        assertThat(string.get(new Locale("eo"))).isEqualTo("c A 1");
        assertThat(string.get(new Locale("en", "US"))).isEqualTo("b A a 1");
    }

    @Test
    public void testWithArgs() {
        MultiLanguageString string = MultiLanguageString.builder()
            .nl("a {0} b {1}")
            .en("b {1} a {0}")
            .in("eo").is("c {0} {1}")
            .defaultLocale(new Locale("nl"))
            .args("A", 1)
            .build();

        assertThat(string.toString()).isEqualTo("a A b 1");
        assertThat(string.get(new Locale("en"))).isEqualTo("b 1 a A");
        assertThat(string.get(new Locale("eo"))).isEqualTo("c A 1");
        assertThat(string.get(new Locale("en", "US"))).isEqualTo("b 1 a A");
    }

    @Test
    public void withBundle() throws Exception {
        try (AutoCloseable autoCloseable = Locales.with(new Locale("eo"))) {
            MultiLanguageString string = MultiLanguageString.builder()
                .nl("a {0} b {1}")
                .en("b {1} a {0}")
                .bundle("nl.vpro.i18n.test.MultiLanguageStringTest", "a")
                .args("A", 1)
                .build();
            assertThat(string.toString()).isEqualTo("esperanto A");
        }
    }

}
