package nl.vpro.jackson3;

import tools.jackson.core.*;
import tools.jackson.databind.*;

import java.time.Instant;
import java.time.ZonedDateTime;

import static nl.vpro.jackson3.DateModule.ZONE;


public class ZonedDateTimeToJsonTimestamp {

    private ZonedDateTimeToJsonTimestamp() {}

    public static class Serializer extends ValueSerializer<ZonedDateTime> {

        public static final Serializer INSTANCE = new Serializer();

        @Override
        public void serialize(ZonedDateTime value, JsonGenerator jgen, SerializationContext ctxt) throws JacksonException {
            if (value == null) {
                jgen.writeNull();
            } else {
                jgen.writeNumber(value.toInstant().toEpochMilli());
            }
        }
    }

    public static class Deserializer extends ValueDeserializer<ZonedDateTime> {

        public static final Deserializer INSTANCE = new Deserializer();

        @Override
        public ZonedDateTime deserialize(JsonParser jp, DeserializationContext ctxt) throws JacksonException {
            try {
                return Instant.ofEpochMilli(jp.getLongValue()).atZone(ZONE);
            } catch (JacksonException jps) {
                String s = jp.getValueAsString();
                if (s == null) {
                    return null;
                }
                return ZonedDateTime.parse(jp.getValueAsString());
            }
        }
    }
}
