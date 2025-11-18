package nl.vpro.test.util.jaxb;


import org.junit.jupiter.api.Test;
import org.w3c.dom.Node;
import org.xmlunit.util.Predicate;

import nl.vpro.test.util.jaxb.test.*;

import static nl.vpro.test.util.jaxb.JAXBTestUtil.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class JAXBTestUtilTest {


    @Test
    public void testMarshal() {
        assertEquals("""
            <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
            <a xmlns="urn:test:1234">
                <value>aa</value>
                <b i="1" j="2">
                    <value>bb</value>
                    <c>cc</c>
                </b>
            </a>
            """, JAXBTestUtil.marshal(new A()));


    }

    @Test
    public void testMarshalWithNoRootElement () {
        assertThatXml(new B()).isSimilarTo("""
            <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
            <local:b xmlns="urn:test:1234" xmlns:local="uri:local" i="1" j="2">
                <value>bb</value>
                <c>cc</c>
            </local:b>""");

    }

    @SuppressWarnings("deprecation")
    @Test
    public void testContainsDeprecated() {
        roundTrip(new A(), """
            <b i="1" j="2">
                    <value>bb</value>
                    <c>cc</c>
                </b>""");
    }


    @Test
    public void testContains() {
        A a = roundTripContains(new A(), " <b i=\"1\" j=\"2\">\n" +
            "        <value>bb</value>\n" +
        /*    "        <map>\n" +
            "            <e>\n" +
            "                <k>x</k>\n" +
            "                <v>y</v>\n" +
            "            </e>\n" +
            "        </map>\n" +*/
            "        <c>cc</c>\n" +
            "    </b>");

        assertThat(a.getB().getI()).isEqualTo(1);
    }

    @Test
    public void testContainsNoNamespace() {
        ANoNamespace a = roundTripContains(new ANoNamespace(),
             "<a>xx</a>",
            "<c>zz</c>");
    }

    @Test
    public void testContainsNoNamespaceFails() {
        assertThatThrownBy(() -> {
            ANoNamespace a = roundTripContains(new ANoNamespace(),
                "<a>xx</a>",
                "<dd>qq</dd>",
                "<c>zz</c>");
        }).isInstanceOf(AssertionError.class);
    }

    @Test
    public void testContainsFluentResult() {
        Result<A> result = assertThatXml(new A()).containsSimilar("""
            <b xmlns="urn:test:1234" i='1' j='2'>
                        <value>bb</value>
                           <c>cc</c>
                     </b>""").getResult();
        assertThat(result.rounded().getB().getJ()).isEqualTo(2);
        assertThatXml(result.xml()).isSimilarTo("""
            <a xmlns="urn:test:1234"><value>aa</value><b i="1" j="2"><value>bb</value><c>cc</c></b></a>
            """);
    }

    @Test
    public void testContainsFluent() {
        A rounded = assertThatXml(new A()).containsSimilar("""
            <b xmlns="urn:test:1234" i='1' j='2'>
                        <value>bb</value>
                           <c>cc</c>
                     </b>""").get();
        assertThat(rounded.getB().getJ()).isEqualTo(2);
    }

    @Test
    public void testContainsFluentFails() {
        assertThatThrownBy(() ->
            assertThatXml(new A()).containsSimilar("""
                <b xmlns="urn:test:1234" i='1' j='3'>
                            <value>bb</value>
                        <c>cc</c>
                </b>"""))
            .isInstanceOf(AssertionError.class);
    }
    @Test
    public void testFluentWithFilter() {
        assertThatXml(new A())
            .withNodeFilter(n -> ! n.getNodeName().equals("value")).isSimilarTo("""
                <a xmlns="urn:test:1234">
                         <value>aa</value>
                         <b i="1" j="2">
                           <value>different</value>
                           <c>cc</c>
                         </b>
                       </a>""");
    }
    @Test
    public void testFluentWithFilterFails() {
        assertThatThrownBy(() ->

            assertThatXml(new A())
            .withNodeFilter(n -> ! n.getNodeName().equals("value")).isSimilarTo("""
                  <a xmlns="urn:test:1234">
                         <value>aa</value>
                         <b i="2" j="2">
                           <value>different</value>
                           <c>cc</c>
                         </b>
                       </a>""")
        ).isInstanceOf(AssertionError.class);
    }

}
