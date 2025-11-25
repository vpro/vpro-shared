package nl.vpro.jackson3;

import java.io.IOException;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.time.format.DateTimeParseException;

import tools.jackson.core.JsonGenerator;
import tools.jackson.core.JsonParseException;
import tools.jackson.core.JsonParser;
import tools.jackson.core.JsonProcessingException;
import tools.jackson.databind.*;
import tools.jackson.databind.JsonDeserializer;
import tools.jackson.databind.JsonSerializer;
import tools.jackson.databind.SerializerProvider;


public class InstantToSecondsFloatTimestamp {

    private InstantToSecondsFloatTimestamp() {}

    public static class Serializer extends ValueSerializer<Instant> {


        public static final Serializer INSTANCE = new Serializer();

        @Override
        public void serialize(Instant value, JsonGenerator jgen, SerializerProvider provider) throws IOException {
            if (value == null) {
                jgen.writeNull();
            } else {
                jgen.writeNumber(value.toEpochMilli() / 1000f);
            }
        }
    }


    public static class Deserializer extends ValueDeserializer<Instant> {

        public static final Deserializer INSTANCE = new Deserializer();
        @Override
        public Instant deserialize(JsonParser jp, DeserializationContext ctxt) {
            try {
                return Instant.ofEpochMilli((long) Float.parseFloat(jp.getValueAsString()) * 1000);
            } catch ( jpe) {
                try {
                    String s = jp.getValueAsString();
                    if (s == null) {
                        return null;
                    }
                    return Instant.parse(s);
                } catch (DateTimeParseException dtps) {
                    return ZonedDateTime.parse(jp.getValueAsString()).toInstant();
                }
            }
        }


    }
}
