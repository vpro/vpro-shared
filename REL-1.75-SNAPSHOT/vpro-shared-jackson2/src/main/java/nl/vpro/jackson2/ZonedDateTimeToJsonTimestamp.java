package nl.vpro.jackson2;

import java.io.IOException;
import java.time.Instant;
import java.time.ZonedDateTime;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;

import static nl.vpro.jackson2.DateModule.ZONE;


public class ZonedDateTimeToJsonTimestamp {

    public static class Serializer extends JsonSerializer<ZonedDateTime> {


        public static Serializer INSTANCE = new Serializer();

        @Override
        public void serialize(ZonedDateTime value, JsonGenerator jgen, SerializerProvider provider) throws IOException {
            if (value == null) {
                jgen.writeNull();
            } else {
                jgen.writeNumber(value.toInstant().toEpochMilli());
            }
        }
    }


    public static class Deserializer extends JsonDeserializer<ZonedDateTime> {

        public static Deserializer INSTANCE = new Deserializer();
        @Override
        public ZonedDateTime deserialize(JsonParser jp, DeserializationContext ctxt) throws IOException, JsonProcessingException {
            try {
                return Instant.ofEpochMilli(jp.getLongValue()).atZone(ZONE);
            } catch (JsonParseException jps) {
                String s = jp.getValueAsString();
                if (s == null) {
                    return null;
                }
                return ZonedDateTime.parse(jp.getValueAsString());
            }
        }
    }
}
