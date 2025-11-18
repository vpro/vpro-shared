/*
 * Copyright (C) 2012 All rights reserved
 * VPRO The Netherlands
 */
package nl.vpro.test.util.jaxb;

import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

import java.io.*;
import java.lang.annotation.Annotation;
import java.net.URL;
import java.util.*;
import java.util.function.*;

import javax.xml.XMLConstants;
import javax.xml.namespace.QName;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.*;
import javax.xml.transform.dom.DOMResult;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.*;
import jakarta.xml.bind.*;
import jakarta.xml.bind.annotation.XmlRootElement;

import org.apache.commons.io.IOUtils;
import org.apache.commons.io.output.StringBuilderWriter;
import org.assertj.core.api.AbstractObjectAssert;
import org.assertj.core.api.Fail;
import org.w3c.dom.Element;
import org.w3c.dom.*;
import org.w3c.dom.ls.DOMImplementationLS;
import org.w3c.dom.ls.LSSerializer;
import org.w3c.dom.traversal.NodeFilter;
import org.xml.sax.*;
import org.xmlunit.XMLUnitException;
import org.xmlunit.builder.DiffBuilder;
import org.xmlunit.diff.*;
import org.xmlunit.util.Predicate;

import nl.vpro.test.util.TestClass;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;


/**
 *
 * @author Roelof Jan Koekoek
 * @since 1.6
 */
@Slf4j
public class JAXBTestUtil {

    private static final String LOCAL_URI = "uri:local";

    public static final Consumer<DiffBuilder> IGNORE_ELEMENT_ORDER = df ->df.withNodeMatcher(new DefaultNodeMatcher(ElementSelectors.byNameAndText));


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

    @SuppressWarnings("unchecked")
    private static <T> void marshal(T object, Consumer<Object> marshaller) {
        Annotation xmlRootElementAnnotation = object.getClass().getAnnotation(XmlRootElement.class);
        if (xmlRootElementAnnotation == null) {
            Class<T> clazz = (Class<T>) object.getClass();
            String tagName = clazz.getSimpleName();
            if (tagName.length() == 1 || (Character.isUpperCase(tagName.charAt(0)) && ! Character.isUpperCase(tagName.charAt(1)))) {
                tagName = Character.toLowerCase(tagName.charAt(0)) + tagName.substring(1);

            }
            marshaller.accept(new JAXBElement<>(
                new QName(LOCAL_URI, tagName, "local"), clazz, object
            ));
        } else {
            marshaller.accept(object);
        }
    }


    private static Marshaller getMarshallerForUnknownClasses(Class<?>... clazz) throws JAXBException {
        JAXBContext context = JAXBContext.newInstance(clazz);
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
     * <p>
     * I prefer a decent error report. The supposed improvement {#link roundTripAndSimilar} returns "expected [true]
     * got [false]" on failure which is a very poor description of what goes wrong and always needs further debugging
     * to investigate the source. Besides that, if a whatever upgrade brakes the layout, while honoring the syntax, I
     * would still like the be informed om this change.
     * <p>
     * MM: The difference was in the order of attributes and/or namespaces. Those are not relevant changes.
     *     SAX implementations are not required to preserve or guarantee any order in this. It is hence impossible to make a test using this that succeeds in every java version.
     *     Furthermore, roundTripAndSimilar will if not similar still do a test for equals to enforce a clearer message.
     *
     * @deprecated  unfeasible for different java versions. (tests which used this where often failing with java 8). Use e.g {#link roundTripContains}
     */
    @Deprecated
    @SuppressWarnings("unchecked")
    public static  <T> T roundTrip(T input, String contains) {
        String xml = marshal(input);
        assertThat(xml).contains(contains);
        return (T)JAXB.unmarshal(new StringReader(xml), input.getClass());
    }


    public static  <T> Result<T> roundTripAndSimilarResult(T input, boolean namespaceAware, String... contains){
        return roundTripAndSimilarResult(input, namespaceAware, builder -> {}, contains);
    }
    @SneakyThrows
    @SuppressWarnings("unchecked")
    public static  <T> Result<T> roundTripAndSimilarResult(T input, boolean namespaceAware, Consumer<DiffBuilder> builderConsumer, String... contains) {
        Element xml = marshalToElement(input);

        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(namespaceAware);
        DocumentBuilder builder = factory.newDocumentBuilder();
        List<Element> elementsToFind = Arrays.stream(contains).map(cont -> {
            try {
                Element elementToFind = builder.parse(new InputSource(new StringReader(cont))).getDocumentElement();
                elementToFind.normalize();
                return elementToFind;
            } catch (SAXException | IOException se) {
                throw new RuntimeException(se);
            }
        }).toList();
        for (Element elementToFind : elementsToFind) {
            NodeList elementsByTagName = xml.getElementsByTagName(elementToFind.getTagName());
            boolean found = false;
            for (int i = 0; i < elementsByTagName.getLength(); i++) {
                Node element = elementsByTagName.item(i);
                element.normalize();
                DiffBuilder diffBuilder = DiffBuilder
                    .compare(elementToFind)
                    .withTest(element)
                    .ignoreWhitespace()
                    .checkForSimilar();
                builderConsumer.accept(diffBuilder);
                Diff diff = diffBuilder.build();
                if (!diff.hasDifferences()) {
                    found = true;
                }
            }
            if (!found) {
                StringBuilderWriter writer = new StringBuilderWriter();
                JAXB.marshal(input, writer);
                assertThat(writer.toString()).contains(contains);
            }
        }


        return new Result<>((T)JAXB.unmarshal(new DOMSource(xml), input.getClass()), () -> {
            String str;
            try {
                DOMImplementationLS lsImpl = (DOMImplementationLS) xml.getOwnerDocument().getImplementation().getFeature("LS", "3.0");
                LSSerializer serializer = lsImpl.createLSSerializer();
                serializer.getDomConfig().setParameter("xml-declaration", false);
                str = serializer.writeToString(xml);
            } catch (Exception e){
                log.warn(e.getMessage());
                str = marshal(input);
            }
            return str;
        });
    }

    /**
     * Checks whether the
     *
     * @since 2.7
     */

    public static  <T> T roundTripContains(T input, boolean namespaceAware, String... contains) {
        return roundTripAndSimilarResult(input, namespaceAware, contains).rounded();
    }


    /**
     * Checks whether marshalled version of an object contains a certain piece of xml.
     *
     * @since 2.7
     */
     public static  <T> T roundTripContains(T input, String... contains) {
         return roundTripContains(input, true, contains);
     }

    public static  <T> Result<T> roundTripContainsResult(T input, String... contains) {
        return roundTripAndSimilarResult(input, true, contains);

    }
    public static  <T> Result<T> roundTripContainsResult(T input, Consumer<DiffBuilder> consumer, String... contains) {
        return roundTripAndSimilarResult(input, true, consumer, contains);
    }



    @SafeVarargs
    public static <T> T roundTripAndSimilar(String input, Class<? extends T> inputClazz, Consumer<DiffBuilder>... build) {
        T result = unmarshal(input, inputClazz);
        String xmlAfter = marshal(result);
        similar(xmlAfter, input, build);
        return result;
    }

    @SafeVarargs
    public static <T> T roundTripAndSimilar(InputStream input, Class<? extends T> inputClazz, Consumer<DiffBuilder>... build) throws IOException {
        StringWriter write = new StringWriter();
        IOUtils.copy(input, write, "UTF-8");
        return roundTripAndSimilar(write.toString(), inputClazz, build);
    }

    /**
     * Marshalls input and checks if it is similar to given string.
     * Then unmarshals it, and marshalls it another time. The result XMl should still be similar.
     */
    @SuppressWarnings({"DuplicatedCode"})
    public static <T> T roundTripAndSimilar(T input, String expected) {
        return roundTripAndSimilarResult(input, expected).rounded();
    }

    /**
     * @since 5.4
     */
    @SuppressWarnings({"DuplicatedCode", "unchecked"})
    public static <T> Result<T> roundTripAndSimilarResult(T input, String expected, Consumer<DiffBuilder>... consumer) {
        String xml = marshal(input);
        similar(xml, expected, consumer);
        Class<? extends T> clazz = (Class<? extends T>) input.getClass();
        T result = unmarshal(xml, clazz);
        /// make sure unmarshalling worked too, by marshalling the result again.
        String xmlAfter = marshal(result);
        similar(xmlAfter, xml, consumer);
        return new Result<>(result, xml);
    }

    @SafeVarargs
    public static <T> T roundTripAndValidateAndSimilar(T input, URL xsd, InputStream expected, Consumer<DiffBuilder>... consumer) throws IOException, SAXException {
        StringWriter write = new StringWriter();
        IOUtils.copy(expected, write, "UTF-8");
        return roundTripAndValidateAndSimilar(input, xsd, write.toString(), consumer);
    }

    @SuppressWarnings("unchecked")
    public static <T> T roundTripAndValidateAndSimilar(T input, URL xsd, String expected, Consumer<DiffBuilder>... consumers) throws IOException, SAXException {
        String xml = null;
        try {
            xml = marshal(input);
            SchemaFactory schemaFactory = SchemaFactory
                .newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
            Schema schema = schemaFactory.newSchema(xsd);
            Validator validator = schema.newValidator();
            validator.validate( new StreamSource(new StringReader(xml)));

            similar(xml, expected, consumers);
            Class<? extends T> clazz = (Class<? extends T>) input.getClass();
            T result = unmarshal(xml, clazz);
            /// make sure unmarshalling worked too, by marshalling the result again.
            String xmlAfter = marshal(result);
            similar(xmlAfter, xml, consumers);
            return result;
        } catch (SAXParseException spe) {
            throw new RuntimeException(
                "input: " + xml + "\n" +
                "expected: " + expected, spe);
        }
    }
    public static <T> T roundTripAndSimilar(T input, InputStream expected) throws IOException {
        StringWriter writer = new StringWriter();
        IOUtils.copy(expected, writer, UTF_8);
        return roundTripAndSimilar(input, writer.toString());
    }

    @SafeVarargs
    public static <T> Result<T> roundTripAndSimilarResult(T input, InputStream expected, Consumer<DiffBuilder>... consumers) throws IOException {
        StringWriter writer = new StringWriter();
        IOUtils.copy(expected, writer, UTF_8);
        return roundTripAndSimilarResult(input, writer.toString(), consumers);
    }


    public static <T> T roundTripAndSimilarAndEquals(T input, String expected) {
        T result = roundTripAndSimilar(input, expected);
        assertThat(result).isEqualTo(input);
        return result;
    }

    @SuppressWarnings("unchecked")
    public static <T> T roundTripAndSimilarValue(T input, String expected) throws JAXBException {
        TestClass<T> embed = new TestClass<>(input);
        Marshaller marshaller = getMarshallerForUnknownClasses(TestClass.class, input.getClass());
        String xml = marshal(marshaller, embed);
        similar(xml, "<testclass>" + expected + "</testclass>");
        TestClass<T> result = (TestClass<T>) JAXB.unmarshal(new StringReader(xml), embed.getClass());
        /// make sure unmarshalling worked too, by marshalling the result again.
        String xmlAfter = marshal(result);
        similar(xmlAfter, xml);
        return result.value;

    }

    @SafeVarargs
    public static void similar(String input, String expected, Consumer<DiffBuilder>... build) {
        similar(input.getBytes(UTF_8), expected.getBytes(UTF_8), build);
    }

    @SafeVarargs
    public static void similar(byte[] input, byte[] expected, Consumer<DiffBuilder>... build) {
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
            assertNoDifferences(diff, input, expected);
        } catch (XMLUnitException xue) {
            Fail.fail(xue.getMessage() + ": expected:\n" + new String(expected, UTF_8) + "\nactual:\n" + new String(input, UTF_8));
        }
    }

    public static <T> T similar(String input, String expected, Class<T> result) {
        similar(input, expected);
        return unmarshal(input, result);
    }

    @SafeVarargs
    @SneakyThrows
    public static void similar(InputStream input, String expected, Consumer<DiffBuilder>... build) {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        IOUtils.copy(input, bytes);
        similar(bytes.toByteArray(), expected.getBytes(UTF_8), build);
    }

    @SafeVarargs
    @SneakyThrows
    public static void similar(String input, InputStream expected, Consumer<DiffBuilder>... build) {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        IOUtils.copy(expected, bytes);
        similar(input.getBytes(UTF_8), bytes.toByteArray(), build);
    }

    @SafeVarargs
    @SneakyThrows
    public static void similar(InputStream input, InputStream expected, Consumer<DiffBuilder>... build) {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        IOUtils.copy(input, bytes);
        ByteArrayOutputStream expectedBytes = new ByteArrayOutputStream();
        IOUtils.copy(expected, expectedBytes);
        similar(bytes.toByteArray(), expectedBytes.toByteArray(), build);
    }


    public static <S extends JAXBTestUtil.XMLObjectAssert<S, T>, T> XMLObjectAssert<S, T> assertThatXml(T o) {
        return new XMLObjectAssert<>(o);
    }

    public static JAXBTestUtil.XMLStringAssert assertThatXml(CharSequence o) {
        return new XMLStringAssert(o);
    }


    public static JAXBTestUtil.XMLStringAssert assertThatXml(ByteArrayOutputStream o) {
        return assertThatXml(o.toByteArray());
    }

    public static JAXBTestUtil.XMLStringAssert assertThatXml(byte[] o) {
        return new XMLStringAssert(new String(o, UTF_8));
    }


    @SuppressWarnings("UnusedReturnValue")
    public static class XMLObjectAssert<S extends XMLObjectAssert<S, A>, A> extends AbstractObjectAssert<S, A> {

        List<Consumer<DiffBuilder>> builders = new ArrayList<>();

        A rounded;

        String xml;

        boolean roundTrip = true;


        public S noRoundTrip() {
            roundTrip = false;
            return myself;
        }

        protected XMLObjectAssert(A actual) {
            super(actual, XMLObjectAssert.class);
        }


        public S withBuilder(Consumer<DiffBuilder> builder) {
            this.builders.add(builder);
            return myself;
        }
        public S withNodeFilter(Predicate<Node> nodeFilter) {
            this.builders.add(df -> df.withNodeFilter(nodeFilter));
            return myself;
        }

        @SuppressWarnings({"CatchMayIgnoreException"})
        public S isSimilarTo(String expected) {
            Consumer<DiffBuilder>[] consumers = builders.toArray(new Consumer[0]);
            if (roundTrip) {
                try {

                    Result<A> result = roundTripAndSimilarResult(actual, expected, consumers);
                    rounded = result.rounded();
                    xml = result.xml();
                } catch (Exception e) {
                    Fail.fail(e.getMessage(), e);
                }
            } else {
                xml = marshal(actual);
                similar(xml, expected, consumers);
            }
            return myself;
        }

        /**
         * @since 5.4
         */
        @SuppressWarnings({"CatchMayIgnoreException"})
        public S isSimilarTo(InputStream expected) {
            Consumer<DiffBuilder>[] consumers = builders.toArray(new Consumer[0]);

            if (roundTrip) {
                try {

                    Result<A> result =  roundTripAndSimilarResult(actual, expected, consumers);
                    rounded = result.rounded();
                    xml = result.xml();
                } catch (Exception e) {
                    Fail.fail(e.getMessage(), e);
                }
            } else {
                xml = marshal(actual);
                similar(xml, expected, consumers);
            }
            return myself;
        }


        @SuppressWarnings({"CatchMayIgnoreException"})
        public S containsSimilar(String expected) {
            try {
                Consumer<DiffBuilder> consumers = (b) -> {
                    for (Consumer<DiffBuilder> c : builders) {
                        c.accept(b);
                    }
                };
                Result<A> result = roundTripContainsResult(actual, consumers, expected);
                rounded = result.rounded;
                xml = result.xml();
            } catch (Exception e) {
                Fail.fail(e.getMessage(), e);
            }
            return myself;
        }

        /**
         * As {@code assertThat(}{@link #get()}{@code )}
         */
        public AbstractObjectAssert<?, A> andRounded() {
            return assertThat(get());
        }

        public S isValid(javax.xml.validation.Validator validator)  {
            try {
                validator.validate(new StreamSource(new StringReader(marshal(rounded))));
            } catch (SAXException | IOException e) {
                Fail.fail(e.getMessage(), e);
            }
            return myself;
        }

        /**
         * Returns the object as it is after marshalling/unmarshalling
         * @return The object after a round trip
         *
         */
        public A get() {
            if (rounded == null) {
                throw new IllegalStateException("No similation was done already.");
            }
            return rounded;
        }

        /**
         * Returns the object as it is after marshalling/unmarshalling
         * @return The object after a round trip
         *
         */
        public String xml() {
            if (xml == null) {
                throw new IllegalStateException("No marshalling was done already.");
            }
            return xml;
        }

        public Result<A> getResult() {
            return new Result(rounded, xml);
        }


    }

    public static class XMLStringAssert extends AbstractObjectAssert<XMLStringAssert, CharSequence> {

        List<Consumer<DiffBuilder>> builders = new ArrayList<>();

        protected XMLStringAssert(CharSequence actual) {
            super(actual, XMLStringAssert.class);
        }

        public XMLStringAssert withBuilder(Consumer<DiffBuilder> builder) {
            this.builders.add(builder);
            return myself;
        }
        public XMLStringAssert withNodeFilter(Predicate<Node> nodeFilter) {
            this.builders.add(df -> df.withNodeFilter(nodeFilter));
            return myself;
        }

        public XMLStringAssert isSimilarTo(String expected) {
            similar(String.valueOf(actual), expected, builders.toArray(new Consumer[0]));
            return myself;
        }


        /**
         * @since 5.4
         */
        public XMLStringAssert isSimilarTo(InputStream expected) {
            similar(String.valueOf(actual), expected);
            return myself;
        }
    }

    /**
     * @since 5.4
     */
    public record Result<A>(A rounded, Supplier<String> xmlSupplier) {

        public Result(A rounded, String xml) {
            this(rounded, () -> xml);
        }

        public String xml() {
            return xmlSupplier.get();
        }

    }


    protected static void assertNoDifferences(Diff diff, byte[] input, byte[] expected) {
        if (diff.hasDifferences()) {
            assertThat(pretty(input)).isEqualTo(pretty(expected));
            //throw new AssertionError(diff.toString() + ": expected:\n" + expected + "\nactual:\n" + input);
        } else {
            assertThat(diff.hasDifferences()).isFalse();
        }
    }
    public static String pretty(String xml) {
        return pretty(xml.getBytes(UTF_8));
    }

    @SafeVarargs
    @SneakyThrows
    public static String pretty(byte[] xml, Consumer<Node>... nodeProcessors) {
        Transformer transformer = TransformerFactory.newInstance().newTransformer(new
            StreamSource(JAXBTestUtil.class.getResourceAsStream("/pretty.xslt")));
        transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");
        transformer.setOutputProperty(OutputKeys.INDENT, "yes");
        if (nodeProcessors.length == 0) {
            StreamResult result = new StreamResult(new StringWriter());
            transformer.transform(new StreamSource(new ByteArrayInputStream(xml)), result);
            String xmlString = result.getWriter().toString();
            return xmlString;
        } else {
            DOMResult domResult = new DOMResult();
            transformer.transform(new StreamSource(new ByteArrayInputStream(xml)), domResult);
            Document doc = (Document) domResult.getNode();
            for (Consumer<Node> nodeProcessor : nodeProcessors) {
                nodeProcessor.accept(doc);
            }
            DOMSource source = new DOMSource(doc);
            StreamResult result = new StreamResult(new StringWriter());
            transformer.transform(source, result);
            String xmlString = result.getWriter().toString();
            return xmlString;
        }
    }

}
