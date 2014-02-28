/**
 * Copyright (C) 2012 All rights reserved
 * VPRO The Netherlands
 */
package nl.vpro.test.util.jaxb;

import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;

import javax.xml.bind.JAXB;

import org.custommonkey.xmlunit.Diff;
import org.custommonkey.xmlunit.XMLUnit;
import org.xml.sax.SAXException;

import static org.fest.assertions.Assertions.assertThat;

/**
 *
 * @author Roelof Jan Koekoek
 * @since 1.6
 */
public class JAXBTestUtil {
    public static String marshal(Object object) {
        StringWriter writer = new StringWriter();
        JAXB.marshal(object, writer);
        return writer.toString();
    }

    public static <T> T unmarshal(String xml, Class<T> clazz) {
        return JAXB.unmarshal(new StringReader(xml), clazz);
    }

    @SuppressWarnings("unchecked")
    public static  <T> T roundTrip(T input) {
        return (T)JAXB.unmarshal(new StringReader(marshal(input)), input.getClass());
    }

    /**
     * @deprecated  unfeasible for different java versions. (tests which used this where often failing with java 8). Use e.g {#link roundTripAndSimilar}
     */
    @SuppressWarnings("unchecked")
    public static  <T> T roundTrip(T input, String contains) {
        String xml = marshal(input);
        assertThat(xml).contains(contains);
        return (T)JAXB.unmarshal(new StringReader(xml), input.getClass());
    }


    public static <T> T roundTripAndSimilar(T input, String expected) throws IOException, SAXException {
        String xml = marshal(input);
        Diff diff = XMLUnit.compareXML(expected, xml);
        assertThat(diff.identical()).isTrue();
        return (T) JAXB.unmarshal(new StringReader(xml), input.getClass());
    }
}
