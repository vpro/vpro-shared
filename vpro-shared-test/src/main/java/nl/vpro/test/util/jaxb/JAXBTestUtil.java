/*
 * Copyright (C) 2012 All rights reserved
 * VPRO The Netherlands
 */
package nl.vpro.test.util.jaxb;

import junit.framework.ComparisonFailure;
import lombok.SneakyThrows;

import java.io.*;
import java.lang.annotation.Annotation;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import javax.xml.XMLConstants;
import javax.xml.bind.*;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.namespace.QName;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.dom.DOMResult;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;

import org.apache.commons.io.IOUtils;
import org.assertj.core.api.AbstractObjectAssert;
import org.assertj.core.api.Fail;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
import org.xmlunit.XMLUnitException;
import org.xmlunit.builder.DiffBuilder;
import org.xmlunit.diff.DefaultNodeMatcher;
import org.xmlunit.diff.Diff;
import org.xmlunit.diff.ElementSelectors;

import nl.vpro.test.util.TestClass;

import static org.assertj.core.api.Assertions.assertThat;


/**
 *
 * @author Roelof Jan Koekoek
 * @since 1.6
 */
@SuppressWarnings("ALL")
public class JAXBTestUtil {

    private static final String LOCAL_URI = "uri:local";

    public static Consumer<DiffBuilder> IGNORE_ELEMENT_ORDER = df ->df.withNodeMatcher(new DefaultNodeMatcher(ElementSelectors.byNameAndText));


    public static <T> String marshal(T object) {
        final StringWriter writer = new StringWriter();
        marshal(object, (o) -> JAXB.marshal(o, writer));
        return writer.toString();
    }

    public static <T> Element marshalToElement(T object) {
        DOMResult writer = new DOMResult();
        marshal(object, (o) -> JAXB.marshal(o, writer));
        return ((Document) writer.getNode()).getDocumentElement();
    }

    private static <T> void marshal(T object, Consumer<Object> marshaller) {
        Annotation xmlRootElementAnnotation = object.getClass().getAnnotation(XmlRootElement.class);
        if (xmlRootElementAnnotation == null) {
            Class<T> clazz = (Class<T>) object.getClass();
            String tagName = clazz.getSimpleName();
            if (tagName.length() == 1 || (Character.isUpperCase(tagName.charAt(0)) && ! Character.isUpperCase(tagName.charAt(1)))) {
                tagName = Character.toLowerCase(tagName.charAt(0)) + tagName.substring(1);

            }
            marshaller.accept(new JAXBElement<T>(
                new QName(LOCAL_URI, tagName, "local"), clazz, object
            ));
        } else {
            marshaller.accept(object);
        }
    }


    private static Marshaller getMarshallerForUnknownClasses(Class<?>... clazz) throws JAXBException {
        JAXBContext context = JAXBContext.newInstance((Class[]) clazz);
        Marshaller marshaller = context.createMarshaller();
        marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
        return marshaller;
    }
    private static String marshal(Marshaller marshaller, Object o) throws JAXBException {
        StringWriter writer = new StringWriter();
        marshaller.marshal(o, writer);
        return writer.toString();
    }

    public static <T> T unmarshal(String xml, Class<? extends T> clazz) {
        return JAXB.unmarshal(new StringReader(xml), clazz);
    }

    @SuppressWarnings("unchecked")
    public static  <T> T roundTrip(T input) {
        return (T)JAXB.unmarshal(new StringReader(marshal(input)), input.getClass());
    }


    /**

     *
     * One might disagree on this subject!
     *
     * I prefer a decent error report. The supposed improvement {#link roundTripAndSimilar} returns "expected [true]
     * got [false]" on failure which is a very poor description of what goes wrong and always needs further debugging
     * to investigate the source. Besides that, if a whatever upgrade brakes the layout, while honoring the syntax, I
     * would still like the be informed om this change.
     *
     * MM: The difference was in the order of attributes and/or namespaces. Those are not relevant changes.
     *     SAX implementations are not required to preserve or guarantee any order in this. It is hence impossible to make a test using this that succeeds in every java version.
     *     Furthermore roundTripAndSimilar will if not similar still do a test for equals to enforce a clearer message.
     *
     * @Deprecated  unfeasible for different java versions. (tests which used this where often failing with java 8). Use e.g {#link roundTripContains}
     */
    @Deprecated
    @SuppressWarnings("unchecked")
    public static  <T> T roundTrip(T input, String contains) {
        String xml = marshal(input);
        assertThat(xml).contains(contains);
        return (T)JAXB.unmarshal(new StringReader(xml), input.getClass());
    }

    /**
     *
     * @since 2.7
     * @param input
     * @param contains
     * @param <T>
     * @return
     */
    @SneakyThrows
    public static  <T> T roundTripContains(T input, String contains, boolean namespaceAware) {
        Element xml = marshalToElement(input);
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(namespaceAware);
        DocumentBuilder builder = factory.newDocumentBuilder();
        Element elementToFind = builder.parse(new InputSource(new StringReader(contains))).getDocumentElement();
        elementToFind.normalize();
        NodeList elementsByTagName = xml.getElementsByTagName(elementToFind.getTagName());
        boolean found = false;
        List<Diff> diffs = new ArrayList<>();
        for (int i = 0; i < elementsByTagName.getLength() ; i++ ) {
            Node element = elementsByTagName.item(i);
            element.normalize();
            Diff diff  = DiffBuilder
                .compare(elementToFind)
                .withTest(element)
                .ignoreWhitespace()
                .checkForSimilar()
                .build();
            diffs.add(diff);
            if (! diff.hasDifferences()) {
                found = true;
            }
        }
        if (! found) {
            Fail.fail(xml + " does not contain " + contains + "\n" + diffs.stream().map(d -> d.toString()).collect(Collectors.toList()));
        }
        return (T)JAXB.unmarshal(new DOMSource(xml), input.getClass());
    }
     public static  <T> T roundTripContains(T input, String contains) {
         return roundTripContains(input, contains, true);
     }


    @SafeVarargs
    public static <T> T roundTripAndSimilar(String input, Class<? extends T> inputClazz, Consumer<DiffBuilder>... build) throws IOException, SAXException {
        try {
            T result = unmarshal(input, inputClazz);
            String xmlAfter = marshal(result);
            similar(xmlAfter, input, build);
            return result;
        } catch (SAXParseException spe) {
            throw new RuntimeException(
                "input: " + input, spe
            );
        }

    }

    public static <T> T roundTripAndSimilar(InputStream input, Class<? extends T> inputClazz, Consumer<DiffBuilder>... build) throws IOException, SAXException {
        StringWriter write = new StringWriter();
        IOUtils.copy(input, write, "UTF-8");
        return roundTripAndSimilar(write.toString(), inputClazz, build);
    }

    /**
     * Marshalls input and checks if it is similar to given string.
     * Then unmarshals it, and marshalls it another time. The result XMl should still be similar.
     */
    public static <T> T roundTripAndSimilar(T input, String expected) throws IOException, SAXException {
        String xml = null;
        try {
            xml = marshal(input);
            similar(xml, expected);
            Class<? extends T> clazz = (Class<? extends T>) input.getClass();
            T result = unmarshal(xml, clazz);
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

    public static <T> T roundTripAndValidateAndSimilar(T input, URL xsd, String expected) throws IOException, SAXException {
        String xml = null;
        try {
            xml = marshal(input);
            SchemaFactory schemaFactory = SchemaFactory
                .newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
            Schema schema = schemaFactory.newSchema(xsd);
            Validator validator = schema.newValidator();
            validator.validate( new StreamSource(new StringReader(xml)));

            similar(xml, expected);
            Class<? extends T> clazz = (Class<? extends T>) input.getClass();
            T result = unmarshal(xml, clazz);
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
    public static <T> T roundTripAndSimilar(T input, InputStream expected) throws IOException, SAXException {

        StringWriter writer = new StringWriter();
        IOUtils.copy(expected, writer, Charset.forName("UTF-8"));
        return roundTripAndSimilar(input, writer.toString());
    }




    public static <T> T roundTripAndSimilarAndEquals(T input, String expected) throws Exception {
        T result = roundTripAndSimilar(input, expected);
        assertThat(result).isEqualTo(input);
        return result;
    }

    public static <T> T roundTripAndSimilarValue(T input, String expected) throws IOException, SAXException, JAXBException {

        TestClass<T> embed = new TestClass<>(input);
        String xml = null;
        try {
            Marshaller marshaller = getMarshallerForUnknownClasses(TestClass.class, input.getClass());

            xml = marshal(marshaller, embed);
            similar(xml, "<testclass>" + expected + "</testclass>");
            TestClass<T> result = (TestClass<T>) JAXB.unmarshal(new StringReader(xml), embed.getClass());
            /// make sure unmarshalling worked too, by marshalling the result again.
            String xmlAfter = marshal(result);
            similar(xmlAfter, xml);
            return result.value;
        } catch (SAXParseException spe) {
            throw new RuntimeException(
                "input: " + xml + "\n" +
                    "expected: " + expected, spe);
        }

    }

    public static void similar(String input, String expected, Consumer<DiffBuilder>... build) throws IOException, SAXException {
        DiffBuilder builder = DiffBuilder
            .compare(expected)
            .withTest(input)
            .ignoreWhitespace()
            .checkForSimilar()
            ;

        for (Consumer<DiffBuilder> b : build) {
            b.accept(builder);
        }
        try {
            Diff diff = builder.build();
            if (diff.hasDifferences()) {
                throw new ComparisonFailure(diff.toString(), expected, input);
            } else {
                assertThat(diff.hasDifferences()).isFalse();
            }
        } catch (XMLUnitException xue) {
            throw new ComparisonFailure(xue.getMessage(), expected, input);
        }
    }

    public static <T> T similar(String input, String expected, Class<T> result) throws IOException, SAXException {
        similar(input, expected);
        return unmarshal(input, result);
    }

    public static void similar(InputStream input, String expected) throws IOException, SAXException {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        Diff diff = DiffBuilder
            .compare(expected)
            .withTest(input)
            .ignoreComments()
            .checkForSimilar()
            .build();
        if (diff.hasDifferences()) {
            throw new ComparisonFailure(diff.toString(), expected, bytes.toString());
        }
    }

    public static void similar(InputStream input, InputStream expected) throws IOException, SAXException {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        IOUtils.copy(input, bytes);
        ByteArrayOutputStream expectedBytes = new ByteArrayOutputStream();
        IOUtils.copy(expected, expectedBytes);
        Diff diff = DiffBuilder
            .compare(expected)
            .withTest(input)
            .checkForSimilar()
            .build();
        if (diff.hasDifferences()) {
            throw new ComparisonFailure(diff.toString(), expectedBytes.toString(), bytes.toString());
        }
    }


    public static <S extends JAXBTestUtil.XMLObjectAssert<S, T>, T> XMLObjectAssert<S, T> assertThatXml(T o) {
        return new XMLObjectAssert<>(o);
    }

    public static JAXBTestUtil.XMLStringAssert assertThatXml(String o) {
        return new XMLStringAssert(o);
    }


    public static JAXBTestUtil.XMLStringAssert assertThatXml(ByteArrayOutputStream o) throws UnsupportedEncodingException {
        return assertThatXml(o.toByteArray());
    }

    public static JAXBTestUtil.XMLStringAssert assertThatXml(byte[] o) throws UnsupportedEncodingException {
        return new XMLStringAssert(new String(o, "UTF-8"));
    }


    public static class XMLObjectAssert<S extends XMLObjectAssert<S, A>, A> extends AbstractObjectAssert<S, A> {

        A rounded;

        protected XMLObjectAssert(A actual) {
            super(actual, XMLObjectAssert.class);
        }

        public S isSimilarTo(String expected) {
            try {
                rounded = roundTripAndSimilar(actual, expected);
            } catch (Exception e) {
                Fail.fail(e.getMessage(), e);
            }
            return myself;
        }
        public S containsSimilar(String expected) {
            try {
                rounded = roundTripContains(actual, expected);
            } catch (Exception e) {
                Fail.fail(e.getMessage(), e);
            }
            return myself;

        }

        public AbstractObjectAssert<?, A> andRounded() {
            if (rounded == null) {
                throw new IllegalStateException("No similation was done already.");
            }
            return assertThat(rounded);
        }

        public S isValid (javax.xml.validation.Validator validator) throws SAXException, IOException {
            validator.validate(new StreamSource(new StringReader(marshal(rounded))));
            return myself;
        }

        public A get() {
            if (rounded == null) {
                throw new IllegalStateException("No similation was done already.");
            }
            return rounded;
        }

    }

    public static class XMLStringAssert extends AbstractObjectAssert<XMLStringAssert, CharSequence> {

        protected XMLStringAssert(CharSequence actual) {
            super(actual, XMLStringAssert.class);
        }

        public XMLStringAssert isSimilarTo(String expected) {
            try {
                similar(String.valueOf(actual), expected);
            } catch (SAXException | IOException e) {
                Fail.fail(e.getMessage());
            }
            return myself;
        }
    }

}
