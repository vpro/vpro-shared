package nl.vpro.i18n;

import java.io.StringReader;
import java.util.Locale;

import javax.xml.bind.JAXB;
import javax.xml.bind.annotation.XmlElement;

import org.junit.Test;
import org.xml.sax.SAXException;

import nl.vpro.test.util.jackson2.Jackson2TestUtil;
import nl.vpro.test.util.jaxb.JAXBTestUtil;

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
        a.string = LocalizedString.of("bla", Locales.DUTCH);
        JAXBTestUtil.roundTripAndSimilar(a, "<local:a xmlns:local=\"uri:local\">\n" +
            "    <string xml:lang=\"nl\">bla</string>\n" +
            "</local:a>");
    }

    @Test
    public void json() {
        A a = new A();
        a.string = LocalizedString.of("bla", Locales.NETHERLANDISH);
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
        assertThat(a.string.getLocale()).isEqualTo(Locales.NETHERLANDISH);
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

}
