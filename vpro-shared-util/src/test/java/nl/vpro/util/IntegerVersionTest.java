package nl.vpro.util;

import java.io.StringReader;
import java.io.StringWriter;

import javax.xml.bind.JAXB;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlRootElement;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

/**
 * @author Michiel Meeuwissen
 * @since 2.23
 */
class IntegerVersionTest {

    @XmlRootElement
    static class A  {

        @XmlAttribute
        IntegerVersion version = null;

    }

    @Test
    public void test() {
        StringWriter out = new StringWriter();
        A a = new A();
        a.version = IntegerVersion.parseIntegers("3.14.15");
        JAXB.marshal(a, out);
        assertThat(out.toString().replaceAll("<\\?.*\\s?>\\s*", "")).matches("<a\\s+version=\"3\\.14\\.15\"\\s*/>\\s*");
        a = JAXB.unmarshal(new StringReader(out.toString().replaceAll("3\\.14\\.15", "2.71.82")), A.class);
        assertThat(a.version).isEqualTo(IntegerVersion.parseIntegers("2.71.82"));
        assertThat(a.version.toFloat()).isEqualTo(2.071082f);
    }


    @Test
    public void testNull() {
        StringWriter out = new StringWriter();
        A a = new A();
        JAXB.marshal(a, out);
        assertThat(out.toString().replaceAll("<\\?.*\\s?>\\s*", "")).matches("<a\\s*/>\\s*");
        a = JAXB.unmarshal(new StringReader(out.toString()), A.class);
        assertThat(a.version).isNull();
    }

}
