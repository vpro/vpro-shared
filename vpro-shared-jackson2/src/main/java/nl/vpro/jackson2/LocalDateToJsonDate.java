package nl.vpro.jackson2;

import java.io.IOException;
import java.time.LocalDate;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;


public class LocalDateToJsonDate {

    public static class Serializer extends JsonSerializer<LocalDate> {


        public static Serializer INSTANCE = new Serializer();

        @Override
        public void serialize(LocalDate value, JsonGenerator jgen, SerializerProvider provider) throws IOException {
            if (value == null) {
                jgen.writeNull();
            } else {
                jgen.writeString(value.toString());
            }
        }
    }


    public static class Deserializer extends JsonDeserializer<LocalDate> {

        public static Deserializer INSTANCE = new Deserializer();

        @Override
        public LocalDate deserialize(JsonParser jp, DeserializationContext ctxt) throws IOException {
            String text = jp.getText();
            if (text == null) {
                return null;
            } else {
                return LocalDate.parse(text);
            }
        }
    }
}
