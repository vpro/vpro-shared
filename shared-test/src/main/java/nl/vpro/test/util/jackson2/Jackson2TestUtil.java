/**
 * Copyright (C) 2014 All rights reserved
 * VPRO The Netherlands
 */
package nl.vpro.test.util.jackson2;

import net.sf.json.test.JSONAssert;

import java.io.IOException;
import java.io.StringWriter;

import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;

import nl.vpro.jackson2.Jackson2Mapper;
import nl.vpro.test.util.TestClass;

import static org.assertj.core.api.Assertions.assertThat;


/**
 * @author Roelof Jan Koekoek
 * @since 3.3
 */
public class Jackson2TestUtil {

    private static final ObjectMapper MAPPER = Jackson2Mapper.getPrettyInstance();

    public static  <T> T roundTrip(T input) throws Exception {
        return roundTrip(input, "");
    }

    public static  <T> T roundTrip(T input, String contains) throws Exception {
        StringWriter writer = new StringWriter();
        MAPPER.writeValue(writer, input);

        String text = writer.toString();
        assertThat(text).contains(contains);

        return (T) MAPPER.readValue(text, input.getClass());
    }

    public static <T> T roundTripAndSimilar(T input, String expected) throws Exception  {
        return roundTripAndSimilar(input, expected, Jackson2Mapper.getInstance().getTypeFactory().constructType(input.getClass()));
    }


    public static <T> T assertJsonEquals(String text, String expected, Class<T> typeReference) throws IOException {
        JSONAssert.assertJsonEquals("\n" + text + "\nis different from expected\n" + expected, expected, text);
        return MAPPER.readValue(text, typeReference);
    }


    protected static <T> T roundTripAndSimilar(T input, String expected, JavaType typeReference) throws Exception {
        StringWriter writer = new StringWriter();
        MAPPER.writeValue(writer, input);

        String text = writer.toString();

        JSONAssert.assertJsonEquals("\n" + text + "\nis different from expected\n" + expected, expected, text);
        return MAPPER.readValue(text, typeReference);

    }

    /**
     * Can be used if the input is not a stand alone json object.
     */
    public static <T> T roundTripAndSimilarValue(T input, String expected) throws Exception {
        TestClass<T> embed = new TestClass<>(input);
        JavaType type = Jackson2Mapper.getInstance().getTypeFactory()
            .constructParametrizedType(TestClass.class, TestClass.class, input.getClass());

        TestClass<T> result = roundTripAndSimilar(embed, "{\"value\": " + expected + "}", type);

        return result.value;
    }

}
