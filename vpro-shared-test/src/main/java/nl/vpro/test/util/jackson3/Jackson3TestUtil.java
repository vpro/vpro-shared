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
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.Stream;

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
 * Some utility for testing json.
 * <p>
 * Originally, this contained only static methods like {@link #assertJsonEquals(String, CharSequence, CharSequence, JsonPointer...)}. But nowadays, there are also 'fluent' versions
 * like {@link #assertThatJson(Object)}.
 *
 * @author Roelof Jan Koekoek
 * @since 3.3
 */
@SuppressWarnings("UnusedReturnValue")
@Slf4j
public class Jackson3TestUtil {

    private static final Jackson3Mapper JACKSON_3_MAPPER = Jackson3Mapper.PRETTY.withSourceInLocation();

    public static final ObjectReader READER = JACKSON_3_MAPPER.reader();
    public static final ObjectMapper MAPPER = JACKSON_3_MAPPER.mapper();


    /**
     * Just compares to json, wrapping {@link JSONAssert#assertEquals(String, String, String, JSONCompareMode)}
     * @param pref A prefix used for the fail message
     * @param expected The expected JSON
     * @param actual The actual JSON
     * @param ignores An array of {@link JsonPointer json pointers} of things int the JSON that are to be ignored in the comparison. These will just be removed from the actual json before comparison
     */

    public static void assertJsonEquals(String pref, CharSequence expected, CharSequence actual, JsonPointer...  ignores) {
        assertJsonEquals(pref, expected, actual, jsonNode ->  remove(jsonNode, ignores));
    }


    /**
     * Just compares to json, wrapping {@link JSONAssert#assertEquals(String, String, String, JSONCompareMode)}
     * @param pref A prefix used for the fail message
     * @param expected The expected JSON
     * @param actual The actual JSON
     * @param consumer
     * @since 5.16
     */

    public static void assertJsonEquals(String pref, CharSequence expected, CharSequence actual, JsonConsumer consumer) {
        try {
            if (consumer != null && consumer != Jackson3TestUtil.JsonConsumer.NOP) {
                JsonNode actualJson = READER.readTree(actual.toString());
                consumer.accept(actualJson);
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


    /**
     * Just compares to json, wrapping {@link JSONAssert#assertEquals(String, String, String, JSONCompareMode)}
     * @param pref A prefix used for the fail message
     * @param expected The expected JSON
     * @param actualJson The actual JSON
     * @param consumer
     * @since 5.16
     */
    @SneakyThrows
    public static void assertJsonEquals(String pref, CharSequence expected, JsonNode actualJson, JsonConsumer consumer) {
        String actual = null;
        try {
            consumer.accept(actualJson);
            actual = MAPPER.writeValueAsString(actualJson);

            JSONAssert.assertEquals(pref + "\n" + actual + "\nis different from expected\n" + expected, String.valueOf(expected), String.valueOf(actual), JSONCompareMode.STRICT);

        } catch (AssertionError fail) {
            log.info(fail.getMessage());
            assertThat(prettify(actual)).isEqualTo(prettify(expected));
        }
    }


    /**
     * Utility to remove a set of JsonPointers from a {@link JsonNode}
     * @param actualJson The actual JSON
     * @param ignores An array of {@link JsonPointer json pointers} of things int the JSON that are to be ignored in the comparison. These will just be removed from the actual json before comparison
     */
    public static void remove(JsonNode actualJson, JsonPointer... ignores) {
        for (JsonPointer ignore : ignores){
            remove(actualJson, ignore);
        }
    }


    public static void consume(JsonNode actualJson, JsonConsumer... consumers) {
        for (JsonConsumer consumer : consumers){
            consumer.accept(actualJson);
        }
    }

    /**
     * @since 5.16
     * @param actualJson
     * @param ignore
     */
    public static void remove(JsonNode actualJson, JsonPointer ignore) {
        if (actualJson == null || ignore == null || ignore.matches()) {
            return;
        }
        JsonNode parent = actualJson.at(ignore.head());
        JsonPointer target = ignore.last();
        if (parent instanceof ObjectNode parentNode) {
            String property = target.getMatchingProperty();
            if (property != null) {
                parentNode.remove(property);
            }
        } else if (parent instanceof ArrayNode parentArray) {
            int index = target.getMatchingIndex();
            if (index >= 0 && index < parentArray.size()) {
                parentArray.remove(index);
            }

        }
    }

    public static void ignore(JsonNode actualJson, JsonPointer ignore) {
        if (actualJson == null || ignore == null || ignore.matches()) {
            return;
        }
        JsonNode parent = actualJson.at(ignore.head());
        JsonPointer target = ignore.last();
        if (parent instanceof ObjectNode parentNode) {
            String property = target.getMatchingProperty();
            if (property != null) {
                parentNode.put(property, "IGNORED");
            }
        } else if (parent instanceof ArrayNode parentArray) {
            int index = target.getMatchingIndex();
            if (index >= 0 && index < parentArray.size()) {
                parentArray.set(index, "IGNORED");
            }

        }
    }


    /**
     * @param test JSON
     * @return Prettyfied version of the same JON
     */
    @PolyNull
    public static String prettify(@PolyNull CharSequence test) {
        if (test == null) {
            return null;
        }
        JsonNode jsonNode = READER.readTree(String.valueOf(test));
        return MAPPER.writeValueAsString(jsonNode);
    }


    /**
     * Defaulting version of {@link #assertJsonEquals(CharSequence, CharSequence, JsonPointer...)}, with empty value for prefix.
     */
    public static void assertJsonEquals(CharSequence expected, CharSequence actual, JsonPointer... ignores) {
        assertJsonEquals("", expected, actual, ignores);
    }

    public static void assertJsonEquals(CharSequence expected, CharSequence actual, JsonConsumer consumer) {
        assertJsonEquals("", expected, actual, consumer);
    }



    /**
     * Defaulting version of {@link #roundTrip(Object, String)}
     */
    public static  <T> T roundTrip(T input) {
        return roundTrip(input, "");
    }

    /**
     * <p>Marshalls input, checks whether it contains a string, and unmarshall it.</p>
     *
     * <p>Checks whether marshalling and unmarshalling happens without errors, and the return value can be checked with other tests.</p>
     * @param <T> type of input
     * @param input Object to marshall
     * @param contains A string expected to be contained in the resulting string
     * @return The remarshalled version of {@code input}
     */
    @SuppressWarnings("unchecked")
    public static  <T> T roundTrip(T input, String contains) {
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
    public static <T> T roundTripAndSimilar(T input, JsonNode  expected) {
        return roundTripAndSimilar(MAPPER, input, expected);
    }

    public static <T> T roundTripAndSimilar(ObjectMapper mapper, T input, String expected, boolean remarshall, JsonPointer... pointers) {
        return roundTripAndSimilar(
            mapper,
            input,
            expected,
            mapper.getTypeFactory().constructType(input.getClass()),
            remarshall,
            (json) -> remove(json, pointers)
        );
    }

    /**
     * @since 5.16
     */

    public static <T> T roundTripAndSimilar(ObjectMapper mapper, T input, String expected, boolean remarshall, JsonConsumer consumer) {
        return roundTripAndSimilar(
            mapper,
            input,
            expected,
            mapper.getTypeFactory().constructType(input.getClass()),
            remarshall,
            consumer
        );
    }
    public static <T> T roundTripAndSimilar(ObjectMapper mapper, T input, String expected)  {
        return roundTripAndSimilar(mapper, input, expected, true, JsonConsumer.NOP);
    }

    public static <T> T roundTripAndSimilar(ObjectMapper mapper, T input, JsonNode expected, boolean remarshall) {
        return roundTripAndSimilar(mapper, input, expected,
            mapper.getTypeFactory().constructType(input.getClass()), remarshall);
    }

    public static <T> T roundTripAndSimilar(ObjectMapper mapper, T input, JsonNode expected) {
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

    public static <T> T roundTripAndSimilarAndEquals(T input, JsonNode expected) {
        T result = roundTripAndSimilar(input, expected);
        assertThat(result).isEqualTo(input);
        return result;
    }

    public static <T> T roundTripAndSimilarAndEquals(ObjectMapper mapper, T input, String expected) {
        T result = roundTripAndSimilar(mapper, input, expected);
        assertThat(result).isEqualTo(input);
        return result;
    }

    public static <T> T assertJsonEquals(String actual, String expected, Class<T> typeReference) {
        assertJsonEquals("",  expected, actual);
        return objectReader(MAPPER, typeReference).readValue(actual);
    }


    protected static <T> T roundTripAndSimilar(T input, String expected, JavaType typeReference, boolean remarshal) {
        return roundTripAndSimilar(MAPPER, input, expected, typeReference, remarshal, JsonConsumer.NOP);
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
    protected static <T> T roundTripAndSimilar(ObjectMapper mapper, T input, String expected, JavaType typeReference, boolean remarshall, JsonConsumer consumer) {
        String marshalled = marshallAndSimilar(mapper, input, expected, consumer);

        T unmarshalled =  mapper.readValue(marshalled, typeReference);
        if (remarshall) {
            StringWriter remarshal = new StringWriter();
            mapper.writeValue(remarshal, unmarshalled);
            String remarshalled = remarshal.toString();
            log.debug("Comparing {} with expected {}", remarshalled, expected);

            assertJsonEquals("REMARSHALLED", expected, remarshalled, consumer);
        }
        return unmarshalled;
    }

    protected static <T> String marshallAndSimilar(ObjectMapper mapper, T input, String expected, JsonConsumer consumer)  {
        StringWriter originalWriter = new StringWriter();
        mapper.writeValue(originalWriter, input);
        String marshalled = originalWriter.toString();

        log.debug("Comparing {} with expected {}", marshalled, expected);
        assertJsonEquals(expected, marshalled, consumer);
        return marshalled;
    }


    protected static <T> T roundTripAndSimilar(ObjectMapper mapper, T input, JsonNode expected, JavaType typeReference, boolean remarshall) {
        return roundTripAndSimilar(mapper, input, mapper.writeValueAsString(expected), typeReference, remarshall, JsonConsumer.NOP);
    }

    /**
     * Can be used if the input is not a stand alone json object. It will implicitely be wrapped in {@link TestClass}
     */
    public static <T> T roundTripAndSimilarValue(T input, String expected) {
        TestClass<T> embed = new TestClass<>(input);
        JavaType type = Jackson3Mapper.INSTANCE.mapper().getTypeFactory()
            .constructParametricType(TestClass.class, input.getClass());

        TestClass<T> result = roundTripAndSimilar(embed, "{\"value\": " + expected + "}", type, true);

        return result.value;
    }

    /**
     * Fluent assertion for an object
     * @param o object to marshall
     * @return
     * @param <T>
     */
    public static <T> JsonObjectAssert<T> assertThatJson(T o) {
        return assertThatJson(MAPPER, o);
    }


    public static JsonStringAssert assertThatJson(byte[] o) {
        return assertThatJson(new String(o, StandardCharsets.UTF_8));
    }
    public static JsonStringAssert assertThatJson(String o) {
        return new JsonStringAssert(MAPPER, o);
    }

    public static <T> JsonObjectAssert<T> assertThatJson(ObjectMapper mapper, T o) {
        return new JsonObjectAssert<>(mapper, o);
    }

    public static <T> JsonObjectAssert<T> assertThatJson(Class<T> o, String value) {
        return assertThatJson(MAPPER, o, value);
    }

    public static <T> JsonObjectAssert<T> assertThatJson(ObjectMapper mapper, Class<T> o, String value) {
        return new JsonObjectAssert<>(mapper, o, value);
    }




    @SuppressWarnings("unchecked")
    public static abstract class JsonAssert<S extends JsonAssert<S, A>, A> extends AbstractObjectAssert<S, A> {

        protected final ObjectMapper mapper;

        final List<JsonConsumer> consumers = new ArrayList<>();

        public JsonAssert(ObjectMapper mapper, A a, Class<?> selfType) {
            super(a, selfType);
            this.mapper = mapper;
        }

        JsonConsumer consumer() {
            return consumers.isEmpty() ? JsonConsumer.NOP : (json) -> consume(json, consumers.toArray(Jackson3TestUtil.JsonConsumer[]::new));
        }


        public S containsKeys(String... keys) {
            JsonNode actualJson = actualJson();
            List<String> notFound = new ArrayList<>();
            for (String key : keys) {
                if (actualJson.get(key) == null) {
                    notFound.add(key);
                }
            }
            assertThat(notFound).withFailMessage("Keys " + notFound + " not found (in " + actualJson + ")").isEmpty();
            return (S) this;
        }

        public S doesNotContainKeys(String... keys) {
            JsonNode actualJson = actualJson();
            List<String> found = new ArrayList<>();

            for (String key : keys) {
                if (actualJson.get(key) != null) {
                    found.add(key);
                }

            }
            assertThat(found).withFailMessage("Unexpected keys" + found + " found (in "+ actualJson + ")").isEmpty();
            return (S) this;
        }
        public S remove(JsonPointer... jsonPointers) {
            consumers.addAll(Stream.of(jsonPointers).map(j -> (JsonConsumer) jsonNode -> Jackson3TestUtil.remove(jsonNode, j)).toList());
            return (S) this;
        }

        public S remove(String... jsonPointers) {
            return remove(Arrays.stream(jsonPointers).map(JsonPointer::compile).toArray(JsonPointer[]::new));
        }

        /**
         * Replaces given JsonPointers with 'IGNORED'
         * @param jsonPointers
         * @see #remove(JsonPointer...)  For removal instead
         * @return
         */
        public S ignore(JsonPointer... jsonPointers) {
            consumers.addAll(Stream.of(jsonPointers).map(j -> (JsonConsumer) jsonNode -> Jackson3TestUtil.ignore(jsonNode, j)).toList());
            return (S) this;
        }

        /**
         * @see #ignore(JsonPointer...)
         */
        public S ignore(String... jsonPointers) {
            return ignore(Arrays.stream(jsonPointers).map(JsonPointer::compile).toArray(JsonPointer[]::new));
        }

        public abstract JsonNode actualJson();

        public S actualJson(JsonConsumer... consumers) {
            JsonNode actualJson = actualJson();
            for (JsonConsumer consumer : consumers) {
                consumer.accept(actualJson);
            }
            return (S) this;
        }

    }

    @FunctionalInterface
    public interface JsonConsumer extends Consumer<JsonNode> {

        JsonConsumer NOP = (js) -> {};

    }

    @SuppressWarnings("UnusedReturnValue")
    public static class JsonObjectAssert<A> extends JsonAssert<JsonObjectAssert<A>, A> implements Supplier<A> {

        A rounded;

        private boolean checkRemarshal = true;

        protected JsonObjectAssert(ObjectMapper mapper, A actual) {
            super(mapper, actual, JsonObjectAssert.class);
        }


        protected JsonObjectAssert(ObjectMapper mapper, Class<A> actual, String string) {
            super(mapper, read(mapper, actual, string), JsonObjectAssert.class);
        }


        @Override
        public JsonNode actualJson() {
            return mapper.valueToTree(actual);
        }


        protected static <A> A read(ObjectMapper mapper, Class<A> actual, String string) {
            return mapper.readValue(string, actual);
        }

        @SuppressWarnings({"CatchMayIgnoreException"})
        public JsonObjectAssert<A> isSimilarTo(String expected) {
            try {
                rounded = roundTripAndSimilar(mapper, actual, expected, checkRemarshal, consumer());
            } catch (Exception e) {
                Fail.fail(e.getMessage(), e);
            }
            return myself;
        }

        public JsonObjectAssert<A> withoutRemarshalling() {
            JsonObjectAssert<A> copy = new JsonObjectAssert<>(mapper, actual);
            copy.checkRemarshal = false;
            return copy;
        }

        public JsonMarshallAssert<A> withoutUnmarshalling() {
            return new JsonMarshallAssert<>(mapper, actual);
        }

        @SuppressWarnings({"CatchMayIgnoreException"})
        public JsonObjectAssert<A> isSimilarTo(JsonNode expected) {
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
    public static class JsonMarshallAssert<A> extends JsonAssert<JsonMarshallAssert<A>, A> implements Supplier<String> {

        String marshalled;

        protected JsonMarshallAssert(ObjectMapper mapper, A a) {
            super(mapper, a, JsonMarshallAssert.class);
        }

        @SneakyThrows
        public JsonMarshallAssert<A> isSimilarTo(String expected)  {
            marshalled = marshallAndSimilar(mapper, actual, expected, consumer());
            return myself;
        }

        @Override
        public String get() {
            if (marshalled == null) {
                throw new IllegalStateException("No similation was done already.");
            }
            return marshalled;
        }

        @Override
        public JsonNode actualJson() {
            return mapper.valueToTree(actual);
        }
    }


    public static class JsonStringAssert extends JsonAssert<JsonStringAssert, CharSequence> {

        private JsonNode actualJson;

        protected JsonStringAssert(ObjectMapper mapper, CharSequence actual) {
            super(mapper, actual, JsonStringAssert.class);
        }

        public JsonStringAssert isSimilarTo(String expected, JsonPointer... ignoresOverride) {
            if (ignoresOverride.length == 0) {
                assertJsonEquals("", expected, actual, consumer());
            } else {
                assertJsonEquals("", expected, actual, ignoresOverride);
            }

            return myself;
        }

        @SneakyThrows
        public JsonNode actualJson() {
            if (actualJson == null) {
                actualJson = mapper.readTree(actual.toString());
            }
            return actualJson;
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
