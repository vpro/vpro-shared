/**
 * Copyright (C) 2014 All rights reserved
 * VPRO The Netherlands
 */
package nl.vpro.test.util.jackson2;

import net.sf.json.test.JSONAssert;

import java.io.StringWriter;

import com.fasterxml.jackson.databind.ObjectMapper;

import nl.vpro.jackson2.Jackson2Mapper;

import static org.fest.assertions.Assertions.assertThat;


/**
 * @author Roelof Jan Koekoek
 * @since 3.3
 */
public class Jackson2TestUtil {

    private static final ObjectMapper MAPPER = Jackson2Mapper.getInstance();

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
        StringWriter writer = new StringWriter();
        MAPPER.writeValue(writer, input);

        String text = writer.toString();

        JSONAssert.assertJsonEquals("\n" + text + "\nis different from expected\n" + expected,  expected, text);

        return (T) MAPPER.readValue(text, input.getClass());

    }
}
