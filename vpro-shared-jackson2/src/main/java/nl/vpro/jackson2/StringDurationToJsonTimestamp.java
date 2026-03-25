/*
 * Copyright (C) 2016 All rights reserved
 * VPRO The Netherlands
 */
package nl.vpro.jackson2;

import java.io.IOException;
import java.time.Duration;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.*;

/**
 * @author rico
 * @since 0.37
 */
public class StringDurationToJsonTimestamp {

    private StringDurationToJsonTimestamp() {}

    public static class Serializer extends JsonSerializer<String> {
        public static final StringDurationToJsonTimestamp.Serializer INSTANCE = new StringDurationToJsonTimestamp.Serializer();

        @Override
        public void serialize(String value, JsonGenerator jgen, SerializerProvider provider) throws IOException {
            if (value == null) {
                jgen.writeNull();
            } else {
                jgen.writeNumber(Duration.parse(value).toMillis());
            }
        }
    }


    public static class Deserializer extends JsonDeserializer<String> {

        public static final StringDurationToJsonTimestamp.Deserializer INSTANCE = new StringDurationToJsonTimestamp.Deserializer();

        @Override
        public String deserialize(JsonParser jp, DeserializationContext ctxt) throws IOException {
            try {
                return Duration.ofMillis(jp.getLongValue()).toString();
            } catch (NumberFormatException e) {
                String s = jp.getValueAsString();
                if (s.isEmpty() && ctxt.isEnabled(DeserializationFeature.ACCEPT_EMPTY_STRING_AS_NULL_OBJECT)) {
                    return null;
                }
                throw e;
            }
        }
    }
}
