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
        IntegerVersion version = IntegerVersion.parseIntegers("3.14.15");

    }

    @Test
    public void test() {
        StringWriter out = new StringWriter();
        JAXB.marshal(new A(), out);
        assertThat(out.toString().replaceAll("<\\?.*\\s?>\\s*", "")).matches("<a\\s+version=\"3\\.14\\.15\"\\s*/>\\s*");
        A a = JAXB.unmarshal(new StringReader(out.toString().replaceAll("3\\.14\\.15", "2.71.82")), A.class);
        assertThat(a.version).isEqualTo(IntegerVersion.parseIntegers("2.71.82"));
        assertThat(a.version.toFloat()).isEqualTo(2.071082f);
    }

}
