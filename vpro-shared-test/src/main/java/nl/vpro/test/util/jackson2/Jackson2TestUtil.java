/*
 * Copyright (C) 2014 All rights reserved
 * VPRO The Netherlands
 */
package nl.vpro.test.util.jackson2;

import lombok.extern.slf4j.Slf4j;
import net.sf.json.test.JSONAssert;

import java.io.IOException;
import java.io.StringWriter;

import org.apache.commons.lang3.StringUtils;
import org.assertj.core.api.AbstractObjectAssert;
import org.assertj.core.api.Fail;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;

import nl.vpro.jackson2.Jackson2Mapper;
import nl.vpro.test.util.TestClass;

import static org.assertj.core.api.Assertions.assertThat;


/**
 * @author Roelof Jan Koekoek
 * @since 3.3
 */
@Slf4j
public class Jackson2TestUtil {

    private static final ObjectMapper MAPPER = Jackson2Mapper.getPrettyInstance();

    public static  <T> T roundTrip(T input) throws Exception {
        return roundTrip(input, "");
    }

    /**
     * <p>Marshalls input, checks whether it contains a string, and unmarshall it.</p>
     *
     * <p>Checks whether marshalling and unmarshalling happens without errors, and the return value can be checked with other tests.</p>
     */
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
     * <p>Marshalls input, checks whether it is similar to expected string, and unmarshall it.
     * </p>
     * <p>Checks whether marshalling and unmarshalling happens without errors, and the return value can be checked with other tests.</p>
     */
    public static <T> T roundTripAndSimilar(T input, String expected) throws Exception  {
        return roundTripAndSimilar(MAPPER, input, expected);
    }

    public static <T> T roundTripAndSimilar(ObjectMapper mapper, T input, String expected) throws Exception {
        return roundTripAndSimilar(mapper, input, expected,
            mapper.getTypeFactory().constructType(input.getClass()));
    }

    /**
     * Marshalls input, checks whether it is similar to expected string, and unmarshall it. This unmarshalled result must be equal to the input.
     * <p>
     * Checks whether marshalling and unmarshalling happens without errors, and the return value can be checked with other tests.
     */
    public static <T> T roundTripAndSimilarAndEquals(T input, String expected) throws Exception {
        T result = roundTripAndSimilar(input, expected);
        assertThat(result).isEqualTo(input);
        return result;
    }

    public static <T> T roundTripAndSimilarAndEquals(ObjectMapper mapper, T input, String expected) throws Exception {
        T result = roundTripAndSimilar(mapper, input, expected);
        assertThat(result).isEqualTo(input);
        return result;
    }

    public static <T> T assertJsonEquals(String text, String expected, Class<T> typeReference) throws IOException {
        JSONAssert.assertJsonEquals("\n" + text + "\nis different from expected\n" + expected, expected, text);
        return MAPPER.readValue(text, typeReference);
    }


    protected static <T> T roundTripAndSimilar(T input, String expected, JavaType typeReference) throws Exception {
        return roundTripAndSimilar(MAPPER, input, expected, typeReference);
    }

    protected static <T> T roundTripAndSimilar(ObjectMapper mapper, T input, String expected, JavaType typeReference) throws Exception {
        StringWriter writer = new StringWriter();
        mapper.writeValue(writer, input);

        String text = writer.toString();

        log.debug("Comparing {} with expected {}", text, expected);
        JSONAssert.assertJsonEquals("\n" + text + "\nis different from expected\n" + expected,
            expected,
            text);
        return mapper.readValue(text, typeReference);
    }

    /**
     * Can be used if the input is not a stand alone json object.
     */
    public static <T> T roundTripAndSimilarValue(T input, String expected) throws Exception {
        TestClass<T> embed = new TestClass<>(input);
        JavaType type = Jackson2Mapper.getInstance().getTypeFactory()
            .constructParametricType(TestClass.class, input.getClass());

        TestClass<T> result = roundTripAndSimilar(embed, "{\"value\": " + expected + "}", type);

        return result.value;
    }

    public static <S extends JsonObjectAssert<S, T>, T> JsonObjectAssert<S, T> assertThatJson(T o) {
        return new JsonObjectAssert<>(o);
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


    public static class JsonObjectAssert<S extends JsonObjectAssert<S, A>, A> extends AbstractObjectAssert<S, A> {

        A rounded;
        private final ObjectMapper mapper;

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


        protected static <A> A read(ObjectMapper mapper, Class<A> actual, String string) {
            try {
                return mapper.readValue(string, actual);
            } catch (IOException e) {
                Fail.fail(e.getMessage());
                return null;
            }
        }

        public S isSimilarTo(String expected) {
            try {
                rounded = roundTripAndSimilar(mapper, actual, expected);
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
        public A get() {
            if (rounded == null) {
                throw new IllegalStateException("No similation was done already.");
            }
            return rounded;
        }

    }

    public static class JsonStringAssert extends AbstractObjectAssert<JsonStringAssert, CharSequence> {

        protected JsonStringAssert(CharSequence actual) {
            super(actual, JsonStringAssert.class);
        }

        public JsonStringAssert isSimilarTo(String expected) {
            JSONAssert.assertJsonEquals("\n" + actual + "\nis different from expected\n" + expected, expected, String.valueOf(actual));
            return myself;
        }
    }

}
