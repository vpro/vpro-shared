/*
 * Copyright (C) 2016 All rights reserved
 * VPRO The Netherlands
 */
package nl.vpro.jackson2;

import java.io.IOException;
import java.time.Duration;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;

/**
 * @author rico
 * @since 0.37
 */
public class StringDurationToJsonTimestamp {

    public static class Serializer extends JsonSerializer<String> {
        public static StringDurationToJsonTimestamp.Serializer INSTANCE = new StringDurationToJsonTimestamp.Serializer();

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

        public static StringDurationToJsonTimestamp.Deserializer INSTANCE = new StringDurationToJsonTimestamp.Deserializer();

        @Override
        public String deserialize(JsonParser jp, DeserializationContext ctxt) throws IOException, JsonProcessingException {
            return Duration.ofMillis(jp.getLongValue()).toString();
        }
    }
}
