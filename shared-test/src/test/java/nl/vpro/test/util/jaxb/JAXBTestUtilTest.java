package nl.vpro.test.util.jaxb;

import org.junit.Test;

import nl.vpro.test.util.jaxb.test.A;
import nl.vpro.test.util.jaxb.test.B;

import static org.junit.Assert.assertEquals;

public class JAXBTestUtilTest {


    @Test
    public void testMarshal() throws Exception {
        assertEquals("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n" +
            "<a xmlns=\"urn:test:1234\">\n" +
            "    <value>aa</value>\n" +
            "    <b>\n" +
            "        <value>bb</value>\n" +
            "        <c>cc</c>\n" +
            "    </b>\n" +
            "</a>\n", JAXBTestUtil.marshal(new A()));


    }

    @Test
    public void testMarshalWithNoRootElement () throws Exception {
        assertEquals("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n" +
            "<local:local xmlns=\"urn:test:1234\" xmlns:local=\"uri:local\">\n" +
            "    <value>bb</value>\n" +
            "    <c>cc</c>\n" +
            "</local:local>\n", JAXBTestUtil.marshal(new B()));


    }
}
