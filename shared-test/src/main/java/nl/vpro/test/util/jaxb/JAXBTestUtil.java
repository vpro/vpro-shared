/**
 * Copyright (C) 2012 All rights reserved
 * VPRO The Netherlands
 */
package nl.vpro.test.util.jaxb;

import java.io.*;
import java.lang.annotation.Annotation;

import javax.xml.bind.*;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.namespace.QName;

import org.custommonkey.xmlunit.Diff;
import org.custommonkey.xmlunit.XMLUnit;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

import static org.fest.assertions.Assertions.assertThat;


/**
 *
 * @author Roelof Jan Koekoek
 * @since 1.6
 */
public class JAXBTestUtil {

    private static final String LOCAL_URI = "uri:local";

    public static <T> String marshal(T object) {
        StringWriter writer = new StringWriter();
        Annotation xmlRootElementAnnotation = object.getClass().getAnnotation(XmlRootElement.class);
        if (xmlRootElementAnnotation == null) {
            Class<T> clazz = (Class<T>) object.getClass();
            String tagName = clazz.getSimpleName();
            if (tagName.length() == 1 || (Character.isUpperCase(tagName.charAt(0)) && ! Character.isUpperCase(tagName.charAt(1)))) {
                tagName = Character.toLowerCase(tagName.charAt(0)) + tagName.substring(1);

            }
            JAXB.marshal(new JAXBElement<>(
                new QName(LOCAL_URI, tagName, "local"), clazz, object
            ), writer);
        } else {
            JAXB.marshal(object, writer);
        }
        return writer.toString();
    }

    private static Marshaller getMarshallerForUnknownClass(Class<? extends Object> clazz) throws JAXBException {
        JAXBContext context = JAXBContext.newInstance(clazz);
        Marshaller marshaller = context.createMarshaller();
        marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
        return marshaller;
    }

    public static <T> T unmarshal(String xml, Class<T> clazz) {
        return JAXB.unmarshal(new StringReader(xml), clazz);
    }

    @SuppressWarnings("unchecked")
    public static  <T> T roundTrip(T input) {
        return (T)JAXB.unmarshal(new StringReader(marshal(input)), input.getClass());
    }

    /**
     * !!!@Deprecated  unfeasible for different java versions. (tests which used this where often failing with java 8). Use e.g {#link roundTripAndSimilar}
     *
     * One might disagree on this subject!
     *
     * I prefer a decent error report. The supposed improvement {#link roundTripAndSimilar} returns "expected [true]
     * got [false]" on failure which is a very poor description of what goes wrong and always needs further debugging
     * to investigate the source. Besides that, if a whatever upgrade brakes the layout, while honoring the syntax, I
     * would still like the be informed om this change.
     *
     * MM: The difference was in the order of attributes and/or namespaces. Those are not relevant changes.
     *     SAX implementations are not required to preserve or guarantee any order in this. It is hence impossible to make a test using this that succeeds in any java version.
     *
     */
    @SuppressWarnings("unchecked")
    public static  <T> T roundTrip(T input, String contains) {
        String xml = marshal(input);
        assertThat(xml).contains(contains);
        return (T)JAXB.unmarshal(new StringReader(xml), input.getClass());
    }


    public static <T> T roundTripAndSimilar(T input, String expected) throws IOException, SAXException {
        String xml = null;
        try {
            xml = marshal(input);
            similar(xml, expected);
            T result = (T) JAXB.unmarshal(new StringReader(xml), input.getClass());
            /// make sure unmarshalling worked too, by marshalling the result again.
            String xmlAfter = marshal(result);
            similar(xmlAfter, xml);
            return result;
        } catch (SAXParseException spe) {
            throw new RuntimeException(
                "input: " + xml + "\n" +
                "expected: " + expected, spe);
        }

    }

    public static void similar(String input, String expected) throws IOException, SAXException {
        Diff diff = XMLUnit.compareXML(expected, input);

        if (!diff.identical()) {
            assertThat(input).isEqualTo(expected);
        } else {
            assertThat(diff.identical()).isTrue();
        }
    }

    public static void similar(InputStream input, String expected) throws IOException, SAXException {
        Diff diff = XMLUnit.compareXML(expected, new InputStreamReader(input));
        if (!diff.identical()) {
            assertThat(input).isEqualTo(expected);
        }
    }

    public static void similar(InputStream input, InputStream expected) throws IOException, SAXException {
        Diff diff = XMLUnit.compareXML(new InputStreamReader(expected), new InputStreamReader(input));
        if (!diff.identical()) {
            assertThat(input).isEqualTo(expected);
        }
    }
}
