package nl.vpro.jackson2;

import java.io.IOException;
import java.util.Date;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;


public class DateToJsonTimestamp {

    public static class Serializer extends JsonSerializer<Date> {


        public static Serializer INSTANCE = new Serializer();

        @Override
        public void serialize(Date value, JsonGenerator jgen, SerializerProvider provider) throws IOException {
            if (value == null) {
                jgen.writeNull();
            } else {
                jgen.writeNumber(value.getTime());
            }
        }
    }


    public static class Deserializer extends StdDeserializer<Date> {

        public static Deserializer INSTANCE = new Deserializer(Date.class);

        protected Deserializer(Class<?> vc) {
            super(vc);
        }

        @Override
        public Date deserialize(JsonParser jp, DeserializationContext ctxt) throws IOException {
            return _parseDate(jp, ctxt);
        }
    }
}
