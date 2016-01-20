package nl.vpro.jackson2;

import java.io.IOException;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.time.format.DateTimeParseException;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;


public class InstantToJsonTimestamp {

    public static class Serializer extends JsonSerializer<Instant> {


        public static Serializer INSTANCE = new Serializer();

        @Override
        public void serialize(Instant value, JsonGenerator jgen, SerializerProvider provider) throws IOException {
            if (value == null) {
                jgen.writeNull();
            } else {
                jgen.writeNumber(value.toEpochMilli());
            }
        }
    }


    public static class Deserializer extends JsonDeserializer<Instant> {

        public static Deserializer INSTANCE = new Deserializer();
        @Override
        public Instant deserialize(JsonParser jp, DeserializationContext ctxt) throws IOException, JsonProcessingException {
            try {
                return Instant.ofEpochMilli(jp.getLongValue());
            } catch (JsonParseException jpe) {
                try {
                    return Instant.parse(jp.getValueAsString());
                } catch (DateTimeParseException dtps) {
                    return ZonedDateTime.parse(jp.getValueAsString()).toInstant();
                }
            }
        }
    }
}
