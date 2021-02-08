package nl.vpro.i18n;

import java.io.StringReader;
import java.util.*;

import javax.xml.bind.JAXB;
import javax.xml.bind.annotation.XmlElement;

import org.junit.jupiter.api.Test;

import nl.vpro.test.util.jackson2.Jackson2TestUtil;
import nl.vpro.test.util.jaxb.JAXBTestUtil;

import static java.nio.charset.StandardCharsets.UTF_8;
import static nl.vpro.i18n.Locales.FLEMISH;
import static nl.vpro.i18n.Locales.NETHERLANDISH;
import static nl.vpro.i18n.LocalizedString.builderOf;
import static nl.vpro.i18n.LocalizedString.of;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Michiel Meeuwissen
 * @since 4.8.2
 */
public class LocalizedStringTest {

    public static class A {
        @XmlElement
        LocalizedString string;
    }

    @Test
    public void xml() {
        A a = new A();
        a.string = of("bla", Locales.DUTCH);
        JAXBTestUtil.roundTripAndSimilar(a, "<local:a xmlns:local=\"uri:local\">\n" +
            "    <string xml:lang=\"nl\">bla</string>\n" +
            "</local:a>");
    }

    @Test
    public void json() {
        A a = new A();
        a.string = of("bla", NETHERLANDISH);
        Jackson2TestUtil.roundTripAndSimilar(a, "{\n" +
            "  \"string\" : {\n" +
            "    \"value\" : \"bla\",\n" +
            "    \"lang\" : \"nl_NL\"\n" +
            "  }\n" +
            "}");

    }

    @Test
    public void unmarshal() {
        A a = JAXB.unmarshal(new StringReader("<local:a xmlns:local=\"uri:local\">\n" +
            "    <string xml:lang=\"nl_NL\">bla</string>\n" +
            "</local:a>"), A.class);
        assertThat(a.string.getLocale()).isEqualTo(NETHERLANDISH);
    }

    @Test
    public void unmarshalAndMarshalNull() {
        LocalizedString.XmlLangAdapter xml = new LocalizedString.XmlLangAdapter();
        assertThat(xml.unmarshal(null)).isNull();
        assertThat(xml.marshal(null)).isNull();
    }

/*
    @Test
    public void adaptXx() {
        assertThat(LocalizedString.adapt("xx")).isEqualTo(new Locale("zxx"));
    }
    @Test
    public void adaptCz() {
        assertThat(LocalizedString.adapt("cz")).isEqualTo(new Locale("cs"));
    }*/

    @Test
    public void adaptNl_NL() {
        assertThat(LocalizedString.adapt("nl_NL")).isEqualTo(new Locale("nl", "NL"));
    }

    @Test
    public void adaptNl_NL_informal() {
        assertThat(LocalizedString.adapt("nl_NL_informal")).isEqualTo(new Locale("nl", "NL", "informal"));
    }

    @Test
    public void adapNull() {
        assertThat(LocalizedString.adapt(null)).isNull();
    }


    @Test
    public void adaptUnknownLanguage() {
        assertThat(LocalizedString.adapt("foo_bar")).isEqualTo(new Locale("foo", "bar"));
    }


    @Test
    public void ofNull() {
        assertThat(of(null, NETHERLANDISH)).isNull();
    }

    @Test
    public void subSequence() {
        assertThat(of("de kat krabt de krullen", NETHERLANDISH).subSequence(3, 6)).isEqualTo(of("kat", NETHERLANDISH));
    }

    @Test
    public void charAt() {
        assertThat(of("de kat krabt de krullen", NETHERLANDISH).charAt(10)).isEqualTo('b');
    }

    @Test
    public void length() {
        assertThat(of("de kat krabt de krullen", NETHERLANDISH).length()).isEqualTo(23);
    }

    @Test
    public void testString() {
        assertThat(of("de kat krabt de krullen", NETHERLANDISH).toString()).isEqualTo("de kat krabt de krullen");
    }

    @Test
    public void get() {
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
    public void equals() {
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
