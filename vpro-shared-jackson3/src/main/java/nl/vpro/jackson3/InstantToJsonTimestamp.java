package nl.vpro.jackson3;

import tools.jackson.core.*;
import tools.jackson.core.exc.InputCoercionException;
import tools.jackson.databind.*;

import java.time.Instant;
import java.time.ZonedDateTime;
import java.time.format.DateTimeParseException;


public class InstantToJsonTimestamp {

    private InstantToJsonTimestamp() {}

    public static class Serializer extends ValueSerializer<Instant> {


        public static final Serializer INSTANCE = new Serializer();

        @Override
        public void serialize(Instant value, JsonGenerator jgen, SerializationContext ctxt) throws JacksonException {
            if (value == null) {
                jgen.writeNull();
            } else {
                jgen.writeNumber(value.toEpochMilli());
            }
        }


    }


    public static class Deserializer extends ValueDeserializer<Instant> {

        public static final Deserializer INSTANCE = new Deserializer();
        @Override
        public Instant deserialize(JsonParser jp, DeserializationContext ctxt)  {
            try {
                return Instant.ofEpochMilli(jp.getLongValue());
            } catch (InputCoercionException jpe) {
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
