package nl.vpro.jackson3;

import tools.jackson.core.*;
import tools.jackson.databind.*;

import java.time.LocalDate;


public class LocalDateToJsonDate {

    private LocalDateToJsonDate() {}

    public static class Serializer extends ValueSerializer<LocalDate> {

        public static final Serializer INSTANCE = new Serializer();

        @Override
        public void serialize(LocalDate value, JsonGenerator jgen, SerializationContext ctxt) throws JacksonException {
            if (value == null) {
                jgen.writeNull();
            } else {
                jgen.writeString(value.toString());
            }
        }
    }


    public static class Deserializer extends ValueDeserializer<LocalDate> {

        public static final Deserializer INSTANCE = new Deserializer();

        @Override
        public LocalDate deserialize(JsonParser jp, DeserializationContext ctxt) {
            String text = jp.getString();
            if (text == null) {
                return null;
            } else {
                return LocalDate.parse(text);
            }
        }
    }
}
