package nl.vpro.test.util.jaxb;


import org.junit.jupiter.api.Test;

import nl.vpro.test.util.jaxb.test.*;

import static nl.vpro.test.util.jaxb.JAXBTestUtil.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertEquals;

class JAXBTestUtilTest {


    @Test
    void marshal() {
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
    void marshalWithNoRootElement () {
        assertThatXml(new B()).isSimilarTo("""
            <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
            <local:b xmlns="urn:test:1234" xmlns:local="uri:local" i="1" j="2">
                <value>bb</value>
                <c>cc</c>
            </local:b>""");

    }

    @Test
    @Deprecated
    void containsDeprecated() {
        roundTrip(new A(), """
            <b i="1" j="2">
                    <value>bb</value>
                    <c>cc</c>
                </b>""");
    }


    @Test
    void contains() {
        /*    "        <map>\n" +
            "            <e>\n" +
            "                <k>x</k>\n" +
            "                <v>y</v>\n" +
            "            </e>\n" +
            "        </map>\n" +*/
        A a = roundTripContains(new A(), """
             <b i="1" j="2">
                    <value>bb</value>
                    <c>cc</c>
                </b>\
            """);

        assertThat(a.getB().getI()).isEqualTo(1);
    }

    @Test
    void containsNoNamespace() {
        ANoNamespace a = roundTripContains(new ANoNamespace(),
             "<a>xx</a>",
            "<c>zz</c>");
    }

    @Test
    void containsNoNamespaceFails() {
        assertThatThrownBy(() -> {
            ANoNamespace a = roundTripContains(new ANoNamespace(),
                "<a>xx</a>",
                "<dd>qq</dd>",
                "<c>zz</c>");
        }).isInstanceOf(AssertionError.class);
    }

    @Test
    void containsFluentResult() {
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
    void containsFluent() {
        A rounded = assertThatXml(new A()).containsSimilar("""
            <b xmlns="urn:test:1234" i='1' j='2'>
                        <value>bb</value>
                           <c>cc</c>
                     </b>""").get();
        assertThat(rounded.getB().getJ()).isEqualTo(2);
    }

    @Test
    void containsFluentFails() {
        assertThatThrownBy(() ->
            assertThatXml(new A()).containsSimilar("""
                <b xmlns="urn:test:1234" i='1' j='3'>
                            <value>bb</value>
                        <c>cc</c>
                </b>"""))
            .isInstanceOf(AssertionError.class);
    }
    @Test
    void fluentWithFilter() {
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
    void fluentWithFilterFails() {
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
