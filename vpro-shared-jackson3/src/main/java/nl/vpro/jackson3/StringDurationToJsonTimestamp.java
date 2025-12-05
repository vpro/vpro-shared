/*
 * Copyright (C) 2016 All rights reserved
 * VPRO The Netherlands
 */
package nl.vpro.jackson3;

import tools.jackson.core.*;
import tools.jackson.databind.*;

import java.time.Duration;

/**
 * @author rico
 * @since 0.37
 */
public class StringDurationToJsonTimestamp {

    private StringDurationToJsonTimestamp() {}

    public static class Serializer extends ValueSerializer<String> {
        public static final StringDurationToJsonTimestamp.Serializer INSTANCE = new StringDurationToJsonTimestamp.Serializer();

        @Override
        public void serialize(String value, JsonGenerator jgen, SerializationContext ctxt) throws JacksonException {
            if (value == null) {
                jgen.writeNull();
            } else {
                jgen.writeNumber(Duration.parse(value).toMillis());
            }
        }

    }


    public static class Deserializer extends ValueDeserializer<String> {

        public static final StringDurationToJsonTimestamp.Deserializer INSTANCE = new StringDurationToJsonTimestamp.Deserializer();

        @Override
        public String deserialize(JsonParser jp, DeserializationContext ctxt)  {
            return Duration.ofMillis(jp.getLongValue()).toString();
        }
    }
}
