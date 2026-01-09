package nl.vpro.jackson3;

import tools.jackson.core.*;
import tools.jackson.databind.*;
import tools.jackson.databind.deser.std.StdDeserializer;

import java.util.Date;


public class DateToJsonTimestamp {

    private DateToJsonTimestamp() {}

    public static class Serializer extends ValueSerializer<Date> {
        public static final Serializer INSTANCE = new Serializer();

        @Override
        public void serialize(Date value, JsonGenerator jgen, SerializationContext ctxt) throws JacksonException {
            if (value == null) {
                jgen.writeNull();
            } else {
                jgen.writeNumber(value.getTime());
            }
        }
    }

    public static class Deserializer extends StdDeserializer<Date> {

        public static final Deserializer INSTANCE = new Deserializer(Date.class);

        protected Deserializer(Class<?> vc) {
            super(vc);
        }

        @Override
        public Date deserialize(JsonParser jp, DeserializationContext ctxt) {
            return _parseDate(jp, ctxt);
        }
    }
}
