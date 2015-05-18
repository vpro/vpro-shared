package nl.vpro.jackson2;

import java.io.IOException;
import java.util.Date;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;


public class DateToJsonTimestamp {

    public static class Serializer extends JsonSerializer<Date> {


        public static Serializer INSTANCE = new Serializer();

        @Override
        public void serialize(Date value, JsonGenerator jgen, SerializerProvider provider) throws IOException {
            jgen.writeNumber(value.getTime());
        }
    }


    public static class Deserializer extends JsonDeserializer<Date> {

        public static Deserializer INSTANCE = new Deserializer();
        @Override
        public Date deserialize(JsonParser jp, DeserializationContext ctxt) throws IOException, JsonProcessingException {
            return new Date(jp.getLongValue());
        }
    }
}
