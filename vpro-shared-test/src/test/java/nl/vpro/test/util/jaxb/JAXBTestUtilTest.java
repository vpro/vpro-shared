package nl.vpro.test.util.jaxb;

import org.junit.Test;

import nl.vpro.test.util.jaxb.test.A;
import nl.vpro.test.util.jaxb.test.B;

import static nl.vpro.test.util.jaxb.JAXBTestUtil.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;

public class JAXBTestUtilTest {


    @Test
    public void testMarshal() {
        assertEquals("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n" +
            "<a xmlns=\"urn:test:1234\">\n" +
            "    <value>aa</value>\n" +
            "    <b i=\"1\" j=\"2\">\n" +
            "        <value>bb</value>\n" +
            "        <c>cc</c>\n" +
            "    </b>\n" +
            "</a>\n", JAXBTestUtil.marshal(new A()));


    }

    @Test
    public void testMarshalWithNoRootElement () {
        assertThatXml(new B()).isSimilarTo("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n" +
            "<local:b xmlns=\"urn:test:1234\" xmlns:local=\"uri:local\" i=\"1\" j=\"2\">\n" +
            "    <value>bb</value>\n" +
            "    <c>cc</c>\n" +
            "</local:b>");

    }

    @Test
    public void testContainsDeprecated() {
        roundTrip(new A(), "<b xmlns=\"urn:test:1234\" i='1' j='2'>\n" +
            "            <value>bb</value>\n" +
                "               <c>cc</c>\n" +
                "         </b>");
    }


    @Test
    public void testContains() {
        A a = roundTripContains(new A(), " <b i=\"1\" j=\"2\">\n" +
            "        <value>bb</value>\n" +
            "        <map>\n" +
            "            <e>\n" +
            "                <k>x</k>\n" +
            "                <v>y</v>\n" +
            "            </e>\n" +
            "        </map>\n" +
            "        <c>cc</c>\n" +
            "    </b>");

        assertThat(a.getB().getI()).isEqualTo(1);
    }
    @Test
    public void testContainsFluent() {
        A rounded = assertThatXml(new A()).containsSimilar("<b xmlns=\"urn:test:1234\" i='1' j='2'>\n" +
            "            <value>bb</value>\n" +
                "               <c>cc</c>\n" +
                "         </b>").get();
        assertThat(rounded.getB().getJ()).isEqualTo(2);
    }

    @Test(expected = AssertionError.class)
    public void testContainsFluentFails() {
        assertThatXml(new A()).containsSimilar("<b xmlns=\"urn:test:1234\" i='1' j='3'>\n" +
            "            <value>bb</value>\n" +
                "        <c>cc</c>\n" +
                "</b>");
    }


}
