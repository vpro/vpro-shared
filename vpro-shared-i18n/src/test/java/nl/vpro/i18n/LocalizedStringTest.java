package nl.vpro.i18n;

import java.io.StringReader;
import java.util.*;

import jakarta.xml.bind.JAXB;
import jakarta.xml.bind.annotation.XmlElement;

import org.junit.jupiter.api.Test;

import nl.vpro.test.util.jackson3.Jackson3TestUtil;
import nl.vpro.test.util.jaxb.JAXBTestUtil;

import static java.nio.charset.StandardCharsets.UTF_8;
import static nl.vpro.i18n.Locales.FLEMISH;
import static nl.vpro.i18n.Locales.NETHERLANDISH;
import static nl.vpro.i18n.LocalizedString.*;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Michiel Meeuwissen
 * @since 4.8.2
 */
class LocalizedStringTest {

    public static class A {
        @XmlElement
        LocalizedString.Impl string;
    }

    @Test
    void xml() {
        A a = new A();
        a.string = impl("bla", Locales.DUTCH);
        JAXBTestUtil.roundTripAndSimilar(a, """
            <local:a xmlns:local="uri:local">
                <string xml:lang="nl">bla</string>
            </local:a>""");
    }

    @Test
    void json() {
        A a = new A();
        a.string = impl("bla", NETHERLANDISH);
        Jackson3TestUtil.roundTripAndSimilar(a, """
            {
              "string" : {
                "value" : "bla",
                "lang" : "nl_NL"
              }
            }""");

    }

    @Test
    void unmarshal() {
        A a = JAXB.unmarshal(new StringReader("""
            <local:a xmlns:local="uri:local">
                <string xml:lang="nl_NL">bla</string>
            </local:a>"""), A.class);
        assertThat(a.string.getLocale()).isEqualTo(NETHERLANDISH);
    }

    @Test
    void unmarshalAndMarshalNull() {
        LocalizedString.XmlLangAdapter xml = new LocalizedString.XmlLangAdapter();
        assertThat(xml.unmarshal(null)).isNull();
        assertThat(xml.marshal(null)).isNull();
    }

/*
    @Test
    void adaptXx() {
        assertThat(LocalizedString.adapt("xx")).isEqualTo(new Locale("zxx"));
    }
    @Test
    void adaptCz() {
        assertThat(LocalizedString.adapt("cz")).isEqualTo(new Locale("cs"));
    }*/

    @Test
    void adaptNl_NL() {
        assertThat(LocalizedString.adapt("nl_NL")).isEqualTo(Locale.of("nl", "NL"));
    }

    @Test
    void adaptNl_NL_informal() {
        assertThat(LocalizedString.adapt("nl_NL_informal")).isEqualTo(Locale.of("nl", "NL", "informal"));
    }

    @Test
    void adapNull() {
        assertThat(LocalizedString.adapt(null)).isNull();
    }


    @Test
    void adaptUnknownLanguage() {
        assertThat(LocalizedString.adapt("foo_bar")).isEqualTo(Locale.of("foo", "bar"));
    }


    @Test
    void ofNull() {
        assertThat(of(null, NETHERLANDISH)).isNull();
    }

    @Test
    void subSequence() {
        assertThat(of("de kat krabt de krullen", NETHERLANDISH).subSequence(3, 6)).isEqualTo(of("kat", NETHERLANDISH));
    }

    @Test
    void charAt() {
        assertThat(of("de kat krabt de krullen", NETHERLANDISH).charAt(10)).isEqualTo('b');
    }

    @Test
    void length() {
        assertThat(of("de kat krabt de krullen", NETHERLANDISH).length()).isEqualTo(23);
    }

    @Test
    void string() {
        assertThat(of("de kat krabt de krullen", NETHERLANDISH).toString()).isEqualTo("de kat krabt de krullen");
    }

    @Test
    void get() {
        Set<LocalizedString> strings = new HashSet<>();
        strings.add(of("hoi", NETHERLANDISH));
        strings.add(of("hey", FLEMISH));
        strings.add(of("hi", Locale.US));
        strings.add(builderOf("hi").locale(Locale.US).charsetName("ISO-5589-1").build());
        assertThat(LocalizedString.get(NETHERLANDISH, strings)).isEqualTo("hoi");
        assertThat(LocalizedString.get(NETHERLANDISH, null)).isNull();
    }

    @SuppressWarnings({"EqualsWithItself", "ConstantConditions"})
    @Test
    void equals() {
        LocalizedString hoi = of("hoi", NETHERLANDISH);
        assertThat(hoi.equals(hoi)).isTrue();
        assertThat(hoi.equals(null)).isFalse();
        assertThat(hoi.equals(new Object())).isFalse();
        assertThat(hoi.equals(of("hoi", Locale.US))).isFalse();
        assertThat(hoi.equals(of("hallo", NETHERLANDISH))).isFalse();
        assertThat(hoi.equals(builderOf("hoi").locale(NETHERLANDISH).charset(UTF_8).build())).isFalse();
        assertThat(hoi.toBuilder().charsetName("UTF-8").build().equals(builderOf("hoi").locale(NETHERLANDISH).charset(UTF_8).build())).isTrue();
    }


}
