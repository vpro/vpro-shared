/*
 * Copyright (C) 2014 All rights reserved
 * VPRO The Netherlands
 */
package nl.vpro.test.util.jackson2;

import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.function.*;
import java.util.stream.Stream;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.assertj.core.api.AbstractObjectAssert;
import org.assertj.core.api.Fail;
import org.checkerframework.checker.nullness.qual.PolyNull;
import org.skyscreamer.jsonassert.JSONAssert;
import org.skyscreamer.jsonassert.JSONCompareMode;

import com.fasterxml.jackson.core.JsonPointer;
import com.fasterxml.jackson.core.StreamReadFeature;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import nl.vpro.jackson2.Jackson2Mapper;
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
public class Jackson2TestUtil {

    private static final ObjectMapper MAPPER =
        Jackson2Mapper.getPrettyStrictInstance();

    private static final ObjectReader JSON_READER = MAPPER.readerFor(JsonNode.class).with(StreamReadFeature.INCLUDE_SOURCE_IN_LOCATION);


    /**
     * Just compares to json, wrapping {@link JSONAssert#assertEquals(String, String, String, JSONCompareMode)}
     * @param pref A prefix used for the fail message
     * @param expected The expected JSON
     * @param actual The actual JSON
     * @param ignores An array of {@link JsonPointer json pointers} of things int the JSON that are to be ignored in the comparison. These will just be removed from the actual json before comparison
     */

    public static void assertJsonEquals(String pref, CharSequence expected, CharSequence actual, JsonPointer...  ignores) {
        assertJsonEquals(pref, expected, actual, jsonNode ->  {return remove(jsonNode, ignores);});
    }


    /**
     * Just compares to json, wrapping {@link JSONAssert#assertEquals(String, String, String, JSONCompareMode)}
     * @param pref A prefix used for the fail message
     * @param expected The expected JSON
     * @param actual The actual JSON
     * @param operator
     * @since 5.16
     */

    public static void assertJsonEquals(String pref, CharSequence expected, CharSequence actual, JsonConsumer operator) {
        assertJsonEquals(pref, expected, actual, operator.asOperator());
    }
    public static void assertJsonEquals(String pref, CharSequence expected, CharSequence actual, JsonOperator operator) {
        try {
            if (operator != null && operator != Jackson2TestUtil.JsonConsumer.NOP) {
                JsonNode actualJson = MAPPER.readTree(actual.toString());
                actualJson = operator.apply(actualJson);
                actual = MAPPER.writeValueAsString(actualJson);
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
    public static JsonNode remove(JsonNode actualJson, JsonPointer... ignores) {
        for (JsonPointer ignore : ignores){
            remove(actualJson, ignore);
        }
        return actualJson;
    }


    public static JsonNode operate(JsonNode actualJson, JsonOperator... operators) {
        for (JsonOperator operator  : operators){
            actualJson = operator.apply(actualJson);
        }
        return actualJson;
    }

    /**
     * @since 5.16
     * @param actualJson
     * @param ignore
     */
    public static JsonNode remove(JsonNode actualJson, JsonPointer ignore) {
        if (actualJson == null || ignore == null || ignore.matches()) {
            return actualJson;
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
        return actualJson;
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
        try {
            JsonNode jsonNode = JSON_READER.readTree(String.valueOf(test));
            return MAPPER.writeValueAsString(jsonNode);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }


    /**
     * Defaulting version of {@link #assertJsonEquals(CharSequence, CharSequence, JsonPointer...)}, with empty value for prefix.
     */
    public static void assertJsonEquals(CharSequence expected, CharSequence actual, JsonPointer... ignores) {
        assertJsonEquals("", expected, actual, ignores);
    }

    public static void assertJsonEquals(CharSequence expected, CharSequence actual, JsonOperator operator) {
        assertJsonEquals("", expected, actual, operator);
    }



    /**
     * Defaulting version of {@link #roundTrip(Object, String)}
     */
    public static  <T> T roundTrip(T input) throws Exception {
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

    public static <T> T roundTripAndSimilar(ObjectMapper mapper, T input, String expected, boolean remarshall, JsonPointer... pointers) throws Exception {
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

    public static <T> T roundTripAndSimilar(ObjectMapper mapper, T input, String expected, boolean remarshall, JsonOperator consumer) {
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
        return roundTripAndSimilar(mapper, input, expected, true, JsonOperator.NOP);
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
        return objectReader(MAPPER, typeReference).readValue(actual, typeReference);
    }


    protected static <T> T roundTripAndSimilar(T input, String expected, JavaType typeReference, boolean remarshal) {
        return roundTripAndSimilar(MAPPER, input, expected, typeReference, remarshal, JsonOperator.NOP);
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
    protected static <T> T roundTripAndSimilar(ObjectMapper mapper, T input, String expected, JavaType typeReference, boolean remarshall, JsonOperator operator) {
        String marshalled = marshallAndSimilar(mapper, input, expected, operator);

        T unmarshalled =  mapper.readValue(marshalled, typeReference);
        if (remarshall) {
            StringWriter remarshal = new StringWriter();
            mapper.writeValue(remarshal, unmarshalled);
            String remarshalled = remarshal.toString();
            log.debug("Comparing {} with expected {}", remarshalled, expected);

            assertJsonEquals("REMARSHALLED", expected, remarshalled, operator);
        }
        return unmarshalled;
    }

    protected static <T> String marshallAndSimilar(ObjectMapper mapper, T input, String expected, JsonOperator operator) throws IOException {
        StringWriter originalWriter = new StringWriter();
        mapper.writeValue(originalWriter, input);
        String marshalled = originalWriter.toString();

        log.debug("Comparing {} with expected {}", marshalled, expected);
        assertJsonEquals(expected, marshalled, operator);
        return marshalled;
    }


    protected static <T> T roundTripAndSimilar(ObjectMapper mapper, T input, JsonNode expected, JavaType typeReference, boolean remarshall) throws Exception {
         return roundTripAndSimilar(mapper, input, mapper.writeValueAsString(expected), typeReference, remarshall, JsonOperator.NOP);
    }

    /**
     * Can be used if the input is not a stand alone json object. It will implicitely be wrapped in {@link TestClass}
     */
    public static <T> T roundTripAndSimilarValue(T input, String expected) {
        TestClass<T> embed = new TestClass<>(input);
        JavaType type = Jackson2Mapper.getInstance().getTypeFactory()
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
        return new JsonObjectAssert<>(o);
    }


    public static JsonStringAssert assertThatJson(byte[] o) {
        return assertThatJson(new String(o, StandardCharsets.UTF_8));
    }
    public static JsonStringAssert assertThatJson(String o) {
        return new JsonStringAssert(o);
    }

    public static <T> JsonObjectAssert<T> assertThatJson(ObjectMapper mapper, T o) {
        return new JsonObjectAssert<>(mapper, o);
    }

    public static <T> JsonObjectAssert<T> assertThatJson(Class<T> o, String value) {
        return new JsonObjectAssert<>(o, value);
    }

    public static <T> JsonObjectAssert<T> assertThatJson(ObjectMapper mapper, Class<T> o, String value) {
        return new JsonObjectAssert<>(mapper, o, value);
    }




    @SuppressWarnings("unchecked")
    public static abstract class JsonAssert<S extends JsonAssert<S, A>, A> extends AbstractObjectAssert<S, A> {

        final List<JsonOperator> operators = new ArrayList<>();

        public JsonAssert(A a, Class<?> selfType) {
            super(a, selfType);
        }

        JsonOperator operator() {
            return operators.isEmpty() ? JsonOperator.NOP : (json) -> Jackson2TestUtil.operate(json, operators.toArray(JsonOperator[]::new));
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
            operators.addAll(Stream.of(jsonPointers).map(j -> (JsonOperator) jsonNode -> Jackson2TestUtil.remove(jsonNode, j)).toList());
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
            operators.addAll(Stream.of(jsonPointers).map(j -> (JsonOperator) jsonNode -> {Jackson2TestUtil.ignore(jsonNode, j); return jsonNode;}).toList());
            return (S) this;
        }
        /**
         * @see #ignore(JsonPointer...)
         */
        public S ignore(String... jsonPointers) {
            return ignore(Arrays.stream(jsonPointers).map(JsonPointer::compile).toArray(JsonPointer[]::new));
        }

        /**
         * Add a consumer to be called before comparison
         * @param consumer
         * @return
         */
        public S beforeComparison(JsonConsumer consumer) {
            return beforeComparisonOperate(consumer.asOperator());
        }

        public S beforeComparisonOperate(JsonOperator operator) {
            operators.add(operator);
            return (S) this;
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

        JsonConsumer NOP = new JsonConsumer() {
            @Override
            public void accept(JsonNode jsonNode) {

            }

            @Override
            public JsonOperator asOperator() {
                return JsonOperator.NOP;
            }
        };


        default JsonOperator asOperator() {
            return (js) -> {
                this.accept(js);
                return js;
            };
        }

    }

    /**
     * Acts on a JsonNode. It can edit and return it, or completely replace it.
     * E.g. when dealing with jsonpath
     *
     */
    @FunctionalInterface
    public interface JsonOperator extends UnaryOperator<JsonNode> {

        JsonOperator NOP = (js) -> js;

    }


    @SuppressWarnings("UnusedReturnValue")
    public static class JsonObjectAssert<A> extends JsonAssert<JsonObjectAssert<A>, A> implements Supplier<A> {

        A rounded;
        private final ObjectMapper mapper;

        private boolean checkRemarshal = true;


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



        @Override
        public JsonNode actualJson() {
            return mapper.valueToTree(actual);
        }



        protected static <A> A read(ObjectMapper mapper, Class<A> actualClass, String string) {
            try {
                return mapper.readValue(string, actualClass);
            } catch (IOException e) {
                Fail.fail(e.getMessage(), e);
                return null;
            }
        }

        @SuppressWarnings({"CatchMayIgnoreException"})
        public JsonObjectAssert<A> isSimilarTo(String expected) {
            try {
                rounded = roundTripAndSimilar(mapper, actual, expected, checkRemarshal, operator());
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
                marshalled = marshallAndSimilar(mapper, actual, expected, operator());
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

        protected JsonStringAssert(CharSequence actual) {
            super(actual, JsonStringAssert.class);
        }

        public JsonStringAssert isSimilarTo(String expected, JsonPointer... ignoresOverride) {
            if (ignoresOverride.length == 0) {
                assertJsonEquals("", expected, actual, operator());
            } else {
                assertJsonEquals("", expected, actual, ignoresOverride);
            }

            return myself;
        }

        @SneakyThrows
        public JsonNode actualJson() {
            if (actualJson == null) {
                actualJson = MAPPER.readTree(actual.toString());
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
