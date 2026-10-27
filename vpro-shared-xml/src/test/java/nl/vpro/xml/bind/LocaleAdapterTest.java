package nl.vpro.xml.bind;

import java.util.Locale;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Michiel Meeuwissen
 * @since 0.47
 */
class LocaleAdapterTest {


    @Test
    void adapt() {
        LocaleAdapter adapter = new LocaleAdapter();
        assertThat(adapter.marshal(new Locale("nl", "NL"))).isEqualTo("nl-NL");
        assertThat(adapter.marshal(new Locale("nl", "NL", "slang"))).isEqualTo("nl-NL-slang");


    }


    @Test
    void unadapt() {
        LocaleAdapter adapter = new LocaleAdapter();
        assertThat(adapter.unmarshal("nl")).isEqualTo(new Locale("nl"));
        assertThat(adapter.unmarshal("nl-NL")).isEqualTo(new Locale("nl", "NL"));
        assertThat(adapter.unmarshal("nl-NL-slang-xy")).isEqualTo(new Locale("nl", "NL", "slang-xy"));


    }

}
