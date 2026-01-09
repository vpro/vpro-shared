/*
 * Copyright (C) 2014 All rights reserved
 * VPRO The Netherlands
 */
package nl.vpro.test.util.jackson3;

import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import tools.jackson.core.JsonPointer;
import tools.jackson.core.StreamReadFeature;
import tools.jackson.databind.*;
import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.ObjectNode;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.function.Supplier;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.assertj.core.api.AbstractObjectAssert;
import org.assertj.core.api.Fail;
import org.checkerframework.checker.nullness.qual.PolyNull;
import org.skyscreamer.jsonassert.JSONAssert;
import org.skyscreamer.jsonassert.JSONCompareMode;

import nl.vpro.jackson3.Jackson3Mapper;
import nl.vpro.test.util.TestClass;

import static org.assertj.core.api.Assertions.assertThat;


/**
 * @author Roelof Jan Koekoek
 * @since 3.3
 */
@SuppressWarnings("UnusedReturnValue")
@Slf4j
public class Jackson3TestUtil {

    private static final Jackson3Mapper JACKSON_3_MAPPER = Jackson3Mapper.PRETTY.withSourceInLocation();
    
    private static final ObjectReader READER = JACKSON_3_MAPPER.reader();
    private static final ObjectMapper MAPPER = JACKSON_3_MAPPER.mapper();


    public static void assertJsonEquals(String pref, CharSequence expected, CharSequence actual, JsonPointer... ignores) {
        try {
            if (ignores.length > 0) {
                JsonNode actualJson = READER.readTree(actual.toString());
                remove(actualJson, ignores);
                actual = MAPPER.writer().writeValueAsString(actualJson);
            }

            JSONAssert.assertEquals(pref + "\n" + actual + "\nis different from expected\n" + expected, String.valueOf(expected), String.valueOf(actual), JSONCompareMode.STRICT);

        } catch (AssertionError fail) {
            log.info(fail.getMessage());
            assertThat(prettify(actual)).isEqualTo(prettify(expected));
        } catch (Exception e) {
            log.error(e.getMessage());
            assertThatJson(actual).isEqualTo(prettify(expected));
        }
    }

    @SneakyThrows
    public static void assertJsonEquals(String pref, CharSequence expected, JsonNode actualJson, JsonPointer... ignores) {
        String actual = null;
        try {
            remove(actualJson, ignores);
            actual = MAPPER.writeValueAsString(actualJson);

            JSONAssert.assertEquals(pref + "\n" + actual + "\nis different from expected\n" + expected, String.valueOf(expected), String.valueOf(actual), JSONCompareMode.STRICT);

        } catch (AssertionError fail) {
            log.info(fail.getMessage());
            assertThat(prettify(actual)).isEqualTo(prettify(expected));
        }
    }

    public static void remove(JsonNode actualJson, JsonPointer... ignores) {
        for (JsonPointer ignore : ignores){
            JsonNode parent = actualJson.at(ignore.head());
            JsonNode at = actualJson.at(ignore);
            if (parent instanceof ObjectNode parentNode) {
                parentNode.remove(ignore.getMatchingProperty());
            } else if (parent instanceof ArrayNode parentArray) {
                parentArray.remove(ignore.getMatchingIndex());
            }
        }
    }

    @PolyNull
    public static String prettify(@PolyNull CharSequence test) {
        if (test == null) {
            return null;
        }
        JsonNode jsonNode = READER.readTree(String.valueOf(test));
        return MAPPER.writeValueAsString(jsonNode);
    }


    public static void assertJsonEquals(CharSequence expected, CharSequence actual, JsonPointer... ignores) {
        assertJsonEquals("", expected, actual, ignores);
    }


    public static  <T> T roundTrip(T input) throws Exception {
        return roundTrip(input, "");
    }

    /**
     * <p>Marshalls input, checks whether it contains a string, and unmarshall it.</p>
     *
     * <p>Checks whether marshalling and unmarshalling happens without errors, and the return value can be checked with other tests.</p>
     */
    @SuppressWarnings("unchecked")
    public static  <T> T roundTrip(T input, String contains) throws Exception {
        StringWriter writer = new StringWriter();
        MAPPER.writeValue(writer, input);

        String text = writer.toString();
        if (StringUtils.isNotEmpty(contains)) {
            assertThat(text).contains(contains);
        }

        return (T) MAPPER.readValue(text, input.getClass());
    }

    /**
     * <p>Marshalls input, checks whether it is similar to expected string, and unmarshals it, then marshall it again, and it should still be similar.
     * </p>
     * <p>Checks whether marshalling and unmarshalling happens without errors, and the return value can be checked with other tests.</p>
     */
    public static <T> T roundTripAndSimilar(T input, String expected) {
        return roundTripAndSimilar(MAPPER, input, expected);
    }
    public static <T> T roundTripAndSimilar(T input, JsonNode  expected) throws Exception  {
        return roundTripAndSimilar(MAPPER, input, expected);
    }

    public static <T> T roundTripAndSimilar(ObjectMapper mapper, T input, String expected, boolean remarshall, JsonPointer... ignores)  {
        return roundTripAndSimilar(
            mapper,
            input,
            expected,
            mapper.getTypeFactory().constructType(input.getClass()),
            remarshall,
            ignores
        );
    }
    public static <T> T roundTripAndSimilar(ObjectMapper mapper, T input, String expected)  {
        return roundTripAndSimilar(mapper, input, expected, true);
    }

    public static <T> T roundTripAndSimilar(ObjectMapper mapper, T input, JsonNode expected, boolean remarshall) throws Exception {
        return roundTripAndSimilar(mapper, input, expected,
            mapper.getTypeFactory().constructType(input.getClass()), remarshall);
    }

    public static <T> T roundTripAndSimilar(ObjectMapper mapper, T input, JsonNode expected) throws Exception {
        return roundTripAndSimilar(mapper, input, expected, true);
    }

    public static <T> T roundTripAndSimilar(ObjectMapper mapper, T input, InputStream  expected) throws Exception {
        StringWriter write = new StringWriter();
        IOUtils.copy(expected, write, "UTF-8");
        return roundTripAndSimilar(mapper, input, write.toString());
    }


    /**
     * Marshalls input, checks whether it is similar to expected string, and unmarshall it. This unmarshalled result must be equal to the input.
     * <p>
     * Checks whether marshalling and unmarshalling happens without errors, and the return value can be checked with other tests.
     */
    public static <T> T roundTripAndSimilarAndEquals(T input, String expected)  {
        T result = roundTripAndSimilar(input, expected);
        assertThat(result).isEqualTo(input);
        return result;
    }

    public static <T> T roundTripAndSimilarAndEquals(T input, JsonNode expected) throws Exception {
        T result = roundTripAndSimilar(input, expected);
        assertThat(result).isEqualTo(input);
        return result;
    }

    public static <T> T roundTripAndSimilarAndEquals(ObjectMapper mapper, T input, String expected) {
        T result = roundTripAndSimilar(mapper, input, expected);
        assertThat(result).isEqualTo(input);
        return result;
    }

    public static <T> T assertJsonEquals(String actual, String expected, Class<T> typeReference) throws IOException {
        assertJsonEquals("",  expected, actual);
        return objectReader(MAPPER, typeReference).readValue(actual);
    }


    protected static <T> T roundTripAndSimilar(T input, String expected, JavaType typeReference, boolean remarshal) {
        return roundTripAndSimilar(MAPPER, input, expected, typeReference, remarshal);
    }

    protected static ObjectReader objectReader(ObjectMapper mapper, JavaType typeReference) {
        return mapper.readerFor(typeReference)
            .with(StreamReadFeature.INCLUDE_SOURCE_IN_LOCATION);

    }
    protected static ObjectReader objectReader(ObjectMapper mapper, Class<?> typeReference) {
        return mapper.readerFor(typeReference)
            .with(StreamReadFeature.INCLUDE_SOURCE_IN_LOCATION);

    }

    @SneakyThrows
    protected static <T> T roundTripAndSimilar(ObjectMapper mapper, T input, String expected, JavaType typeReference, boolean remarshall, JsonPointer... ignores) {
        String marshalled = marshallAndSimilar(mapper, input, expected, ignores);

        T unmarshalled =  mapper.readValue(marshalled, typeReference);
        if (remarshall) {
            StringWriter remarshal = new StringWriter();
            mapper.writeValue(remarshal, unmarshalled);
            String remarshalled = remarshal.toString();
            log.debug("Comparing {} with expected {}", remarshalled, expected);

            assertJsonEquals("REMARSHALLED", expected, remarshalled, ignores);
        }
        return unmarshalled;
    }

    protected static <T> String marshallAndSimilar(ObjectMapper mapper, T input, String expected, JsonPointer... ignores) throws IOException {
        StringWriter originalWriter = new StringWriter();
        mapper.writeValue(originalWriter, input);
        String marshalled = originalWriter.toString();

        log.debug("Comparing {} with expected {}", marshalled, expected);
        assertJsonEquals(expected, marshalled, ignores);
        return marshalled;
    }


    protected static <T> T roundTripAndSimilar(ObjectMapper mapper, T input, JsonNode expected, JavaType typeReference, boolean remarshall) throws Exception {
         return roundTripAndSimilar(mapper, input, mapper.writeValueAsString(expected), typeReference, remarshall);
    }

    /**
     * Can be used if the input is not a stand alone json object.
     */
    public static <T> T roundTripAndSimilarValue(T input, String expected) {
        TestClass<T> embed = new TestClass<>(input);
        JavaType type = Jackson3Mapper.INSTANCE.mapper().getTypeFactory()
            .constructParametricType(TestClass.class, input.getClass());

        TestClass<T> result = roundTripAndSimilar(embed, "{\"value\": " + expected + "}", type, true);

        return result.value;
    }

    public static <S extends JsonObjectAssert<S, T>, T> JsonObjectAssert<S, T> assertThatJson(T o) {
        return new JsonObjectAssert<>(o);
    }


    public static JsonStringAssert assertThatJson(byte[] o) {
        return assertThatJson(new String(o, StandardCharsets.UTF_8));
    }
    public static <S extends JsonObjectAssert<S, T>, T> JsonObjectAssert<S, T> assertThatJson(ObjectMapper mapper, T o) {
        return new JsonObjectAssert<>(mapper, o);
    }

    public static <S extends JsonObjectAssert<S, T>, T> JsonObjectAssert<S, T> assertThatJson(Class<T> o, String value) {
        return new JsonObjectAssert<>(o, value);
    }

    public static <S extends JsonObjectAssert<S, T>, T> JsonObjectAssert<S, T> assertThatJson(ObjectMapper mapper, Class<T> o, String value) {
        return new JsonObjectAssert<>(mapper, o, value);
    }

    public static JsonStringAssert assertThatJson(String o) {
        return new JsonStringAssert(o);
    }


    @SuppressWarnings("UnusedReturnValue")
    public static class JsonObjectAssert<S extends JsonObjectAssert<S, A>, A> extends AbstractObjectAssert<S, A> implements Supplier<A> {

        A rounded;
        private final ObjectMapper mapper;

        private boolean checkRemarshal = true;

        private List<JsonPointer> ignores = new ArrayList<>();

        protected JsonObjectAssert(A actual) {
            super(actual, JsonObjectAssert.class);
            this.mapper = MAPPER;
        }


        protected JsonObjectAssert(Class<A> actual, String string) {
            super(read(MAPPER, actual, string), JsonObjectAssert.class);
            this.mapper = MAPPER;
        }

        protected JsonObjectAssert(ObjectMapper mapper, A actual) {
            super(actual, JsonObjectAssert.class);
            this.mapper = mapper;
        }


        protected JsonObjectAssert(ObjectMapper mapper, Class<A> actual, String string) {
            super(read(mapper, actual, string), JsonObjectAssert.class);
            this.mapper = mapper;
        }

        public JsonObjectAssert<S, A> ignore(JsonPointer... jsonPointers) {
            ignores.addAll(Arrays.asList(jsonPointers));
            return this;
        }

        public JsonObjectAssert<S, A> ignore(String... jsonPointers) {
            Arrays.stream(jsonPointers).map(JsonPointer::compile).forEach(jp -> ignores.add(jp));
            return this;
        }



        protected static <A> A read(ObjectMapper mapper, Class<A> actual, String string) {
            return mapper.readValue(string, actual);
        }

        @SuppressWarnings({"CatchMayIgnoreException"})
        public S isSimilarTo(String expected) {
            try {
                rounded = roundTripAndSimilar(mapper, actual, expected, checkRemarshal, ignores.toArray(i -> new JsonPointer[i]));
            } catch (Exception e) {
                Fail.fail(e.getMessage(), e);
            }
            return myself;
        }

        public JsonObjectAssert<S, A> withoutRemarshalling() {
            JsonObjectAssert<S,A> copy = new JsonObjectAssert<>(mapper, actual);
            copy.checkRemarshal = false;
            return copy;
        }
        public JsonMarshallAssert<A> withoutUnmarshalling() {
            return new JsonMarshallAssert<>(mapper, actual);
        }

        @SuppressWarnings({"CatchMayIgnoreException"})
        public S isSimilarTo(JsonNode expected) {
            try {
                rounded = roundTripAndSimilar(mapper, actual, expected, checkRemarshal);
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
        public A get() {
            if (rounded == null) {
                throw new IllegalStateException("No similation was done already.");
            }
            return rounded;
        }

    }
    public static class JsonMarshallAssert<A>
        extends AbstractObjectAssert<JsonMarshallAssert<A>, A> implements Supplier<String> {

        final ObjectMapper mapper;
        String marshalled;

        protected JsonMarshallAssert(A a) {
            this(MAPPER, a);
        }

        protected JsonMarshallAssert(ObjectMapper mapper, A a) {
            super(a, JsonMarshallAssert.class);
            this.mapper = mapper;
        }

        @SneakyThrows
        public JsonMarshallAssert<A> isSimilarTo(String expected)  {
            marshalled = marshallAndSimilar(mapper, actual, expected);
            return myself;
        }

        @Override
        public String get() {
            if (marshalled == null) {
                throw new IllegalStateException("No similation was done already.");
            }
            return marshalled;
        }
    }


    public static class JsonStringAssert extends AbstractObjectAssert<JsonStringAssert, CharSequence> {

        private JsonNode actualJson;

        protected JsonStringAssert(CharSequence actual) {
            super(actual, JsonStringAssert.class);
        }

        public JsonStringAssert isSimilarTo(String expected, JsonPointer... ignores) {
            assertJsonEquals("", expected, actual, ignores);
            return myself;
        }

        public JsonStringAssert containsKeys(String... keys) {
            actualJson();
            List<String> notFound = new ArrayList<>();
            for (String key : keys) {
                if (actualJson.get(key) == null) {
                    notFound.add(key);
                }
            }
            assertThat(notFound).withFailMessage("Keys " + notFound + " found (in " + actualJson + ")").isEmpty();
            return myself;
        }

        public JsonStringAssert doesNotContainKeys(String... keys) {
            actualJson();
            List<String> found = new ArrayList<>();

            for (String key : keys) {
                if (actualJson.get(key) != null) {
                    found.add(key);
                }

            }
            assertThat(found).withFailMessage("Unexpected keys" + found + " found (in "+ actualJson + ")").isEmpty();
            return myself;
        }

        @SneakyThrows
        protected void actualJson() {
            if (actualJson == null) {
                actualJson = MAPPER.readTree(actual.toString());
            }
        }

        public JsonStringAssert isSimilarToResource(String resource) {
            try {
                InputStream resourceAsStream = getClass().getResourceAsStream(resource);
                if (resourceAsStream == null) {
                    throw new IllegalArgumentException("No such resource " + resource);
                }
                String expected = IOUtils.toString(resourceAsStream, StandardCharsets.UTF_8);
                return isSimilarTo(expected);
            } catch (IOException ioe) {
                throw new RuntimeException(ioe);
            }

        }
    }

}
