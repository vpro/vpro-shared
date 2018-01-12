/*
 * Copyright (C) 2012 All rights reserved
 * VPRO The Netherlands
 */
package nl.vpro.test.util.jaxb;

import junit.framework.ComparisonFailure;

import java.io.*;
import java.lang.annotation.Annotation;
import java.util.function.Consumer;

import javax.xml.bind.*;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.namespace.QName;
import javax.xml.transform.stream.StreamSource;

import org.apache.commons.io.IOUtils;
import org.assertj.core.api.AbstractObjectAssert;
import org.assertj.core.api.Fail;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
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
     * @Deprecated  unfeasible for different java versions. (tests which used this where often failing with java 8). Use e.g {#link roundTripAndSimilar}
     */
    @Deprecated
    @SuppressWarnings("unchecked")
    public static  <T> T roundTrip(T input, String contains) {
        String xml = marshal(input);
        assertThat(xml).contains(contains);
        return (T)JAXB.unmarshal(new StringReader(xml), input.getClass());
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

        Diff diff = builder.build();
        if (diff.hasDifferences()) {
            throw new ComparisonFailure(diff.toString(), expected, input);
        } else {
            assertThat(diff.hasDifferences()).isFalse();
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
                Fail.fail(e.getMessage());
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
